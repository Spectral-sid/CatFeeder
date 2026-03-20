package com.sid.catfeeder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.sid.catfeeder.adapters.FeedingAdapter
import com.sid.catfeeder.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private lateinit var btnScan: Button
    private lateinit var tvBarcode: TextView
    private lateinit var etFoodName: EditText
    // private lateinit var etAmount: EditText
    private lateinit var btnSave: Button
    private lateinit var rvHistory: RecyclerView
    private lateinit var llPetsContainer: LinearLayout
    private lateinit var tvSelectedCount: TextView
    private lateinit var tvServerStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var btnRefreshHistory: ImageButton
    private lateinit var adapter: FeedingAdapter
    private lateinit var btnAddFood: Button
    private lateinit var rgPortion: RadioGroup
    private lateinit var rbPortion2: RadioButton
    private lateinit var rbPortion1: RadioButton
    private lateinit var rbPortionHalf: RadioButton
    private lateinit var rbPortionThird: RadioButton
    private lateinit var rbPortionQuarter: RadioButton
    private lateinit var llCalculatedWeight: LinearLayout
    private lateinit var tvCalculatedWeight: TextView
    private lateinit var tvPerPet: TextView

    // Текущий вес корма из последнего сканирования
    private var currentFoodWeight: Double? = null
    private var petsList = mutableListOf<Pet>()
    private var selectedPetIds = mutableSetOf<Int>() // Множество выбранных ID
    private var petCheckboxes = mutableListOf<CheckBox>()
    private var allFeedings = mutableListOf<FeedingHistoryItem>() // Кеш всех записей

    private val PERMISSION_REQUEST_CAMERA = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        checkServerConnection()
        loadPets()
        setupClickListeners()
        checkPermissions()
    }

    private fun initViews() {
        btnScan = findViewById(R.id.btn_scan)
        tvBarcode = findViewById(R.id.tv_barcode)
        etFoodName = findViewById(R.id.et_food_name)
       // etAmount = findViewById(R.id.et_amount)
        btnSave = findViewById(R.id.btn_save)
        rvHistory = findViewById(R.id.rv_history)
        llPetsContainer = findViewById(R.id.llPetsContainer)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        tvServerStatus = findViewById(R.id.tv_server_status)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        btnRefreshHistory = findViewById(R.id.btn_refresh_history)
        btnAddFood = findViewById(R.id.btn_AddFood)

        rgPortion = findViewById(R.id.rgPortion)
        rbPortion2 = findViewById(R.id.rbPortion2)
        rbPortion1 = findViewById(R.id.rbPortion1)
        rbPortionHalf = findViewById(R.id.rbPortionHalf)
        rbPortionThird = findViewById(R.id.rbPortionThird)
        rbPortionQuarter = findViewById(R.id.rbPortionQuarter)
        llCalculatedWeight = findViewById(R.id.llCalculatedWeight)
        tvCalculatedWeight = findViewById(R.id.tvCalculatedWeight)
        tvPerPet = findViewById(R.id.tvPerPet)
    }

    private fun setupRecyclerView() {
        adapter = FeedingAdapter(emptyList()) { feedingItem ->
            showWasFinishedDialog(feedingItem)
        }
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter
    }

    private fun setupClickListeners() {
        btnScan.setOnClickListener {
            startBarcodeScanner()
        }

        btnSave.setOnClickListener {
            saveFeedingForSelectedPets()
        }

        btnRefreshHistory.setOnClickListener {
            loadAllFeedingHistory()
        }
        btnAddFood.setOnClickListener {
            val intent = Intent(this, AddFoodActivity::class.java)
            startActivity(intent)
        }
        // Слушатель для RadioGroup
        rgPortion.setOnCheckedChangeListener { _, _ ->
            calculatePortion()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CAMERA
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешение на камеру получено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Разрешение на камеру отклонено", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkServerConnection() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.checkHealth()
                if (response.isSuccessful && response.body()?.success == true) {
                    tvServerStatus.text = "Сервер подключен ✓"
                    tvServerStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                } else {
                    tvServerStatus.text = "Сервер не отвечает"
                    tvServerStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            } catch (e: Exception) {
                tvServerStatus.text = "Ошибка подключения: ${e.message}"
                tvServerStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            }
        }
    }

    private fun loadPets() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPets()
                showLoading(false)

                if (response.isSuccessful && response.body()?.success == true) {
                    val pets = response.body()?.data ?: emptyList()
                    petsList.clear()
                    petsList.addAll(pets)

                    // По умолчанию выбираем всех питомцев
                    selectedPetIds.clear()
                    selectedPetIds.addAll(pets.map { it.id })

                    updatePetsContainer(pets)
                    updateSelectedCount()

                    // Загружаем историю для всех выбранных
                    loadAllFeedingHistory()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка загрузки питомцев",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@MainActivity,
                    "Ошибка сети: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updatePetsContainer(pets: List<Pet>) {
        llPetsContainer.removeAllViews()
        petCheckboxes.clear()

        pets.forEach { pet ->
            val petView = layoutInflater.inflate(R.layout.item_pet_checkbox, llPetsContainer, false)
            val cardView = petView as MaterialCardView
            val cbPet = cardView.findViewById<CheckBox>(R.id.cbPet)
            val tvPetName = cardView.findViewById<TextView>(R.id.tvPetName)
            val tvPetWeight = cardView.findViewById<TextView>(R.id.tvPetWeight)

            tvPetName.text = pet.name
            tvPetWeight.text = pet.currentWeight?.let { "(${it} кг)" } ?: ""

            // По умолчанию выбран
            cbPet.isChecked = true
            cbPet.tag = pet.id

            cbPet.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPetIds.add(pet.id)
                } else {
                    selectedPetIds.remove(pet.id)
                }
                updateSelectedCount()

                // Визуальный фидбек - меняем цвет карточки
                if (isChecked) {
                    cardView.setStrokeColor(resources.getColor(R.color.purple_500))
                } else {
                    cardView.setStrokeColor(resources.getColor(android.R.color.darker_gray))
                }

                // Обновляем историю при изменении выбора
                filterHistoryBySelectedPets()
            }

            // Визуальный фидбек - цветная обводка для выбранных
            cardView.setStrokeColor(resources.getColor(R.color.purple_500))

            petCheckboxes.add(cbPet)
            llPetsContainer.addView(cardView)
        }
    }

    private fun updateSelectedCount() {
        val count = selectedPetIds.size
        tvSelectedCount.text = "Выбрано питомцев: $count"

        // Если ни один не выбран, показываем предупреждение
        if (count == 0) {
            tvSelectedCount.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        } else {
            tvSelectedCount.setTextColor(resources.getColor(android.R.color.darker_gray))
        }
        // Пересчитываем порцию при изменении количества питомцев
        calculatePortion()
    }

    private fun showWasFinishedDialog(feedingItem: FeedingHistoryItem) {
        WasFinishedDialog(this, feedingItem) { wasFinished ->
            updateWasFinished(feedingItem.id, wasFinished)
        }.show()
    }

    private fun updateWasFinished(feedingId: Int, wasFinished: Int) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateWasFinished(
                    feedingId,
                    WasFinishedRequest(wasFinished)
                )

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@MainActivity,
                            "Статус обновлен",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Обновляем элемент в локальном кеше
                        val index = allFeedings.indexOfFirst { it.id == feedingId }
                        if (index >= 0) {
                            val updatedItem = allFeedings[index].copy(wasFinished = wasFinished)
                            allFeedings[index] = updatedItem
                        }

                        // Обновляем отображение
                        filterHistoryBySelectedPets()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка обновления",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка сети: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun loadAllFeedingHistory() {
        if (petsList.isEmpty()) return

        showLoading(true)
        adapter.updateData(emptyList())
        tvEmptyState.visibility = View.VISIBLE
        tvEmptyState.text = "Загрузка истории кормлений..."

        lifecycleScope.launch {
            try {
                // Загружаем историю для всех питомцев
                val response = RetrofitClient.instance.getAllFeedingHistory(limit = 50)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        allFeedings = (response.body()?.data ?: emptyList()).toMutableList()
                        filterHistoryBySelectedPets()
                    } else {
                        tvEmptyState.visibility = View.VISIBLE
                        tvEmptyState.text = "Ошибка загрузки истории"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Ошибка сети: ${e.message}"
                }
            }
        }
    }

    private fun filterHistoryBySelectedPets() {
        if (selectedPetIds.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Выберите питомцев для просмотра истории!"
            adapter.updateData(emptyList())
            return
        }
        if (allFeedings.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Загрузка истории..."
            return
        }
        val filteredHistory = allFeedings
            .filter { it.petId in selectedPetIds }
            .sortedByDescending { "${it.date} ${it.time ?: ""}" }
            .take(20) // Показываем последние 20 записей

        if (filteredHistory.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Нет записей о кормлении для выбранных питомцев"
            adapter.updateData(emptyList())
        } else {
            tvEmptyState.visibility = View.GONE
            adapter.updateData(filteredHistory)
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show()
        } else {
            val barcode = result.contents
            tvBarcode.text = "Штрихкод: $barcode"
            searchFoodByBarcode(barcode)
        }
    }

    private val addFoodLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Корм успешно добавлен
            val barcode = result.data?.getStringExtra("BARCODE")
            //val foodName = result.data?.getStringExtra("FOOD_NAME")

//            if (barcode != null && foodName != null) {
            if (barcode != null ) {
                searchFoodByBarcode(barcode)
                /*
                // Заполняем поля
                tvBarcode.text = "Штрихкод: $barcode"
                etFoodName.setText(foodName)

                // Также можно попытаться найти корм на сервере, чтобы получить и другие данные
//                searchFoodByBarcode(barcode)
                */
                Toast.makeText(
                    this,
                    "✅ Корм успешно добавлен!",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            // Пользователь отменил добавление
            val barcode = result.data?.getStringExtra("BARCODE")
            if (barcode != null) {
                // Показываем снова диалог "не найден"
                showFoodNotFoundDialog(barcode)
            }
        }
    }

    private fun startBarcodeScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(
            ScanOptions.EAN_13,
            ScanOptions.EAN_8,
            ScanOptions.UPC_A,
            ScanOptions.UPC_E,
            ScanOptions.CODE_39,
            ScanOptions.CODE_128
        )
        options.setPrompt("Наведите на штрихкод")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(false)
        options.setTimeout(15000)

        barcodeLauncher.launch(options)
    }

    private fun searchFoodByBarcode(barcode: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getFoodByBarcode(barcode)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val food = response.body()?.data
                        if (food != null) {
                            etFoodName.setText("${food.manufacturer}: ${food.name} ${food.flavor ?: ""} ${food.weight}г".trim())

                            // Сохраняем вес корма
                            currentFoodWeight = food.weight

                            // Если вес известен, показываем панель расчета
                            if (food.weight != null && food.weight > 0) {
                                llCalculatedWeight.visibility = View.VISIBLE
                                calculatePortion()
                            } else {
                                llCalculatedWeight.visibility = View.GONE
                                Toast.makeText(
                                    this@MainActivity,
                                    "Вес пакета не указан, укажите количество вручную",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            Toast.makeText(
                                this@MainActivity,
                                "Корм найден: ${food.manufacturer} ${food.name} ${food.flavor} ${food.weight}г",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // Корм не найден - показываем диалог
                            showFoodNotFoundDialog(barcode)
                            /*
                            etFoodName.setText("")
                            etFoodName.hint = "Введите название корма"
                            Toast.makeText(
                                this@MainActivity,
                                "Корм не найден в базе. Введите данные вручную.",
                                Toast.LENGTH_LONG
                            ).show()

                             */
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка поиска корма",
                            Toast.LENGTH_LONG
                        ).show()
                        showFoodNotFoundDialog(barcode)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка сети: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun showFoodNotFoundDialog(barcode: String) {
        // Создаем диалог
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("❌ Корм не найден")
            .setMessage("Штрихкод: $barcode\n\nТакой корм отсутствует в базе. Хотите добавить его?")
            .setCancelable(false)
            .create()

        // Настраиваем кнопки
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "➕ Добавить") { _, _ ->
            // Открываем AddFoodActivity и передаем штрихкод
            openAddFoodActivity(barcode)
        }

        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "📷 Сканировать снова") { _, _ ->
            startBarcodeScanner()
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "✕ Закрыть") { _, _ ->
            dialog.dismiss()
            clearFields()
        }

        dialog.show()

        // Настройка цветов кнопок (опционально)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.success_green))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.info_blue))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.text_secondary))
    }

    private fun openAddFoodActivity(barcode: String) {
        val intent = Intent(this, AddFoodActivity::class.java).apply {
            putExtra("BARCODE", barcode)  // Передаем штрихкод
            putExtra("MODE", "add")       // Режим добавления
        }
        addFoodLauncher.launch(intent)  // Используем новый лаунчер
    }

    private fun calculatePortion() {
        if (currentFoodWeight == null || currentFoodWeight!! <= 0) {
            llCalculatedWeight.visibility = View.GONE
            return
        }

        val selectedPetsCount = selectedPetIds.size
        if (selectedPetsCount == 0) {
            tvPerPet.text = ""
            return
        }

        // Определяем долю от веса пакета
        val portionFactor = when (rgPortion.checkedRadioButtonId) {
            R.id.rbPortion2 -> 2.0      // 2 целых пакета
            R.id.rbPortion1 -> 1.0      // 1 целый пакет
            R.id.rbPortionHalf -> 0.5   // Половина пакета
            R.id.rbPortionThird -> 1.0/3.0 // Треть пакета
            R.id.rbPortionQuarter -> 0.25 // Четверть пакета
            else -> 1.0
        }

        val totalPortionWeight = currentFoodWeight!! * portionFactor
        val perPetWeight = totalPortionWeight / selectedPetsCount

        // Обновляем UI
        tvCalculatedWeight.text = String.format("%.0f г", totalPortionWeight)
        tvPerPet.text = String.format("(по %.0f г на питомца)", perPetWeight)

        llCalculatedWeight.visibility = View.VISIBLE
    }

    private fun saveFeedingForSelectedPets() {
        // Проверяем выбран ли хотя бы один питомец
        if (selectedPetIds.isEmpty()) {
            Toast.makeText(this, "Выберите хотя бы одного питомца", Toast.LENGTH_SHORT).show()
            return
        }

        val barcode = tvBarcode.text.toString().replace("Штрихкод: ", "")
        val foodName = etFoodName.text.toString()
        //val totalAmount = etAmount.text.toString().toDoubleOrNull()

        if (barcode == "не отсканирован" || barcode.isEmpty()) {
            Toast.makeText(this, "Отсканируйте штрихкод корма", Toast.LENGTH_SHORT).show()
            return
        }

        if (foodName.isEmpty()) {
            Toast.makeText(this, "Введите название корма", Toast.LENGTH_SHORT).show()
            return
        }

        // Получаем общий вес порции
        val totalPortionWeight = if (currentFoodWeight != null && currentFoodWeight!! > 0) {
            // Рассчитываем из выбранной доли
            val portionFactor = when (rgPortion.checkedRadioButtonId) {
                R.id.rbPortion2 -> 2.0
                R.id.rbPortion1 -> 1.0
                R.id.rbPortionHalf -> 0.5
                R.id.rbPortionThird -> 1.0/3.0
                R.id.rbPortionQuarter -> 0.25
                else -> 1.0
            }
            currentFoodWeight!! * portionFactor
        } else {
            // Если вес неизвестен, показываем ошибку
            Toast.makeText(this, "Вес пакета неизвестен", Toast.LENGTH_SHORT).show()
            return
        }

        // Рассчитываем количество на каждого питомца
        val amountPerPet = totalPortionWeight / selectedPetIds.size
        val formattedTotal = String.format("%.1f", totalPortionWeight)
        val formattedPerPet = String.format("%.1f", amountPerPet)

        // Диалог подтверждения
        AlertDialog.Builder(this)
            .setTitle("Подтверждение")
            .setMessage("Общая порция: ${formattedTotal}г\n" +
                    "Питомцев выбрано: ${selectedPetIds.size}\n" +
                    "Каждому: ${formattedPerPet}г\n\n" +
                    "Записать кормление для всех выбранных питомцев?")
            .setPositiveButton("Да") { _, _ ->
                saveFeedings(selectedPetIds.toList(), totalPortionWeight, amountPerPet, barcode, foodName)
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun saveFeedings(
        petIds: List<Int>,
        totalAmount: Double,
        amountPerPet: Double,
        barcode: String,
        foodName: String
    ) {
        showLoading(true)
        var successCount = 0
        var errorCount = 0
        val newFeedings = mutableListOf<FeedingHistoryItem>()

        lifecycleScope.launch {
            try {
                // Отправляем запросы параллельно
                val jobs = petIds.map { petId ->
                    async {
                        try {
                            val pet = petsList.find { it.id == petId }
                            val feedingLog = FeedingLog(
                                petId = petId,
                                amount = amountPerPet,
                                barcode = barcode,
                                foodName = foodName,
                                notes = "Общее количество: ${totalAmount}г, разделено на ${petIds.size} питомцев"
                            )

                            val response = RetrofitClient.instance.logFeeding(feedingLog)
                            if (response.isSuccessful && response.body()?.success == true) {
                                val id = response.body()?.data?.id
                                if (id != null) {
                                    // Создаем элемент для локального обновления
                                    val newItem = FeedingHistoryItem(
                                        id = id,
                                        date = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date()),
                                        time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()),
                                        foodName = foodName,
                                        barcode = barcode,
                                        amount = amountPerPet,
                                        petId = petId,
                                        petName = pet?.name ?: "Питомец"
                                    )
                                    newFeedings.add(newItem)
                                }
                                successCount++
                            } else {
                                errorCount++
                            }
                        } catch (e: Exception) {
                            errorCount++
                        }
                    }
                }

                // Ждем завершения всех запросов
                jobs.awaitAll()

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    val message = when {
                        errorCount == 0 -> "✅ Кормление записано для всех $successCount питомцев!"
                        successCount > 0 -> "⚠️ Записано для $successCount питомцев, ошибок: $errorCount"
                        else -> "❌ Не удалось записать кормление"
                    }

                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()

                    if (successCount > 0) {
                        clearFields()
                        // Добавляем новые записи в кеш
                        allFeedings.addAll(0, newFeedings)
                        // Обновляем отображение
                        filterHistoryBySelectedPets()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка сети: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun clearFields() {
        tvBarcode.text = "Штрихкод: не отсканирован"
        etFoodName.text.clear()
        // Сбрасываем выбор на "1"
        rbPortion1.isChecked = true
        currentFoodWeight = null
        llCalculatedWeight.visibility = View.GONE
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !isLoading
        btnScan.isEnabled = !isLoading
        btnRefreshHistory.isEnabled = !isLoading
    }
}