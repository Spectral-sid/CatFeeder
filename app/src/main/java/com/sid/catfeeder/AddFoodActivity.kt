package com.sid.catfeeder

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.sid.catfeeder.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sid.catfeeder.models.Flavor
import com.sid.catfeeder.models.FoodType
import com.sid.catfeeder.models.Manufacturer

class AddFoodActivity : AppCompatActivity() {

    private lateinit var etBarcode: EditText
    private lateinit var etName: EditText
    private lateinit var spinnerManufacturer: Spinner
    private lateinit var spinnerFoodType: Spinner
    private lateinit var spinnerFlavor: Spinner
    private lateinit var etWeight: EditText
    private lateinit var etCalories: EditText
    private lateinit var etProtein: EditText
    private lateinit var etFat: EditText
    private lateinit var btnSave: Button
    private lateinit var btnScan: Button
    private lateinit var btnAddManufacturer: ImageButton
    private lateinit var btnAddFlavor: ImageButton
    private lateinit var progressBar: ProgressBar

    private lateinit var apiService: CatFeederApiService

    // Списки для справочников
    private var manufacturers = mutableListOf<Manufacturer>()
    private var foodTypes = mutableListOf<FoodType>()
    private var flavors = mutableListOf<Flavor>()

    // Флаг: редактируем существующий корм или создаем новый
    private var isEditMode = false
    private var existingFoodId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)

        initViews()

        // Получаем штрихкод из интента (при добавлении нового корма)
        val barcode = intent.getStringExtra("BARCODE")
        if (!barcode.isNullOrEmpty()) {
            etBarcode.setText(barcode)
            etBarcode.isEnabled = false
            searchFoodByBarcode(barcode)
        }

        setupApi()
        loadReferenceData()
        setupListeners()
    }

    private fun initViews() {
        etBarcode = findViewById(R.id.etBarcode)
        etName = findViewById(R.id.etName)
        spinnerManufacturer = findViewById(R.id.spinnerManufacturer)
        spinnerFoodType = findViewById(R.id.spinnerFoodType)
        spinnerFlavor = findViewById(R.id.spinnerFlavor)
        etWeight = findViewById(R.id.etWeight)
        etCalories = findViewById(R.id.etCalories)
        etProtein = findViewById(R.id.etProtein)
        etFat = findViewById(R.id.etFat)
        btnSave = findViewById(R.id.btnSave)
        btnScan = findViewById(R.id.btnScan)
        btnAddManufacturer = findViewById(R.id.btnAddManufacturer)
        btnAddFlavor = findViewById(R.id.btnAddFlavor)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupApi() {
        apiService = RetrofitClient.instance
    }

    private fun loadReferenceData() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                loadManufacturers()
                loadFoodTypes()
                loadFlavors()
                setupSpinners()
            } catch (e: Exception) {
                Toast.makeText(this@AddFoodActivity,
                    "Ошибка загрузки справочников: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadManufacturers() {
        val response = apiService.getManufacturers()
        if (response.isSuccessful && response.body()?.success == true) {
            val data = response.body()?.data
            if (data is List<*>) {
                manufacturers.clear()
                manufacturers.addAll(data.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        Manufacturer(
                            id = (item["id"] as? Double)?.toInt() ?: 0,
                            name = item["name"] as? String ?: "",
                            country = item["country"] as? String
                        )
                    } else null
                })
            }
        }
    }

    private suspend fun loadFoodTypes() {
        val response = apiService.getFoodTypes()
        if (response.isSuccessful && response.body()?.success == true) {
            val data = response.body()?.data
            if (data is List<*>) {
                foodTypes.clear()
                foodTypes.addAll(data.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        FoodType(
                            id = (item["id"] as? Double)?.toInt() ?: 0,
                            name = item["name"] as? String ?: ""
                        )
                    } else null
                })
            }
        }
    }

    private suspend fun loadFlavors() {
        val response = apiService.getFlavors()
        if (response.isSuccessful && response.body()?.success == true) {
            val data = response.body()?.data
            if (data is List<*>) {
                flavors.clear()
                flavors.addAll(data.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        Flavor(
                            id = (item["id"] as? Double)?.toInt() ?: 0,
                            name = item["name"] as? String ?: ""
                        )
                    } else null
                })
            }
        }
    }

    private fun setupSpinners() {
        setupManufacturerSpinner()
        setupFoodTypeSpinner()
        setupFlavorSpinner()
    }

    private fun setupManufacturerSpinner() {
        val manufacturerNames = manufacturers.map { it.name }.toMutableList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, manufacturerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerManufacturer.adapter = adapter
    }

    private fun setupFoodTypeSpinner() {
        val typeNames = foodTypes.map { it.name }.toTypedArray()
        if (typeNames.isNotEmpty()) {
            ArrayAdapter(this, android.R.layout.simple_spinner_item, typeNames).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFoodType.adapter = adapter
            }
        }
    }

    private fun setupFlavorSpinner() {
        val flavorNames = mutableListOf("Не указан")
        flavorNames.addAll(flavors.map { it.name })
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, flavorNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFlavor.adapter = adapter
    }

    private fun setupListeners() {
        btnScan.setOnClickListener {
            startBarcodeScanner()
        }

        btnSave.setOnClickListener {
            if (isEditMode) {
                updateFood()
            } else {
                saveFood()
            }
        }

        btnAddManufacturer.setOnClickListener {
            showAddManufacturerDialog()
        }

        btnAddFlavor.setOnClickListener {
            showAddFlavorDialog()
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

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show()
        } else {
            val barcode = result.contents
            etBarcode.setText(barcode)
            etBarcode.isEnabled = false
            searchFoodByBarcode(barcode)
        }
    }

    private fun searchFoodByBarcode(barcode: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = apiService.getFoodByBarcode(barcode)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val food = response.body()?.data
                        if (food != null) {
                            // Корм найден - переходим в режим редактирования
                            isEditMode = true
                            existingFoodId = food.id
                            fillFoodData(food)
                            btnSave.text = "Обновить"
                            Toast.makeText(
                                this@AddFoodActivity,
                                "Корм найден, вы можете отредактировать данные",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // Корм не найден - режим добавления нового
                            isEditMode = false
                            existingFoodId = null
                            btnSave.text = "Сохранить"
                            Toast.makeText(
                                this@AddFoodActivity,
                                "Корм не найден. Заполните данные для добавления",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        isEditMode = false
                        existingFoodId = null
                        btnSave.text = "Сохранить"
                        Toast.makeText(
                            this@AddFoodActivity,
                            "Корм не найден. Заполните данные для добавления",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isEditMode = false
                    existingFoodId = null
                    btnSave.text = "Сохранить"
                    Toast.makeText(
                        this@AddFoodActivity,
                        "Корм не найден. Заполните данные для добавления",
                        Toast.LENGTH_LONG
                    ).show()
                    // логирование ошибки для отладки
                    e.printStackTrace()
                }
            } finally {
                showLoading(false)
            }
        }
    }

    private fun fillFoodData(food: Food) {
        // Заполняем все поля данными из найденного корма
        etName.setText(food.name)

        // Устанавливаем производителя
        val manufacturerIndex = manufacturers.indexOfFirst { it.name == food.manufacturer }
        if (manufacturerIndex >= 0) {
            spinnerManufacturer.setSelection(manufacturerIndex)
        }

        // Устанавливаем тип корма
        val typeIndex = foodTypes.indexOfFirst { it.name == food.type }
        if (typeIndex >= 0) {
            spinnerFoodType.setSelection(typeIndex)
        }

        // Устанавливаем вкус (если есть)
        food.flavor?.let { flavor ->
            val flavorIndex = flavors.indexOfFirst { it.name == flavor }
            if (flavorIndex >= 0) {
                spinnerFlavor.setSelection(flavorIndex + 1) // +1 из-за "Не указан"
            }
        }

        // Заполняем числовые поля (ИСПРАВЛЕНО: теперь заполняются все поля)
        food.weight?.let { etWeight.setText(it.toString()) }
        food.calories?.let { etCalories.setText(it.toString()) }
        food.protein?.let { etProtein.setText(it.toString()) }
        food.fat?.let { etFat.setText(it.toString()) }

        // Разблокируем поля для редактирования
        etBarcode.isEnabled = false  // Штрихкод нельзя менять
        etName.isEnabled = true
        spinnerManufacturer.isEnabled = true
        spinnerFoodType.isEnabled = true
        spinnerFlavor.isEnabled = true
        etWeight.isEnabled = true
        etCalories.isEnabled = true
        etProtein.isEnabled = true
        etFat.isEnabled = true
    }

    private fun saveFood() {
        // Валидация
        if (etBarcode.text.isNullOrBlank()) {
            etBarcode.error = "Введите штрихкод"
            return
        }

        if (etName.text.isNullOrBlank()) {
            etName.error = "Введите название корма"
            return
        }

        if (manufacturers.isEmpty()) {
            Toast.makeText(this, "Справочники еще не загружены", Toast.LENGTH_SHORT).show()
            return
        }

        val foodData = FoodCreate(
            barcode = etBarcode.text.toString(),
            name = etName.text.toString(),
            manufacturerId = manufacturers[spinnerManufacturer.selectedItemPosition].id,
            foodTypeId = foodTypes[spinnerFoodType.selectedItemPosition].id,
            flavorId = if (spinnerFlavor.selectedItemPosition > 0)
                flavors[spinnerFlavor.selectedItemPosition - 1].id else null,
            weight = etWeight.text.toString().toDoubleOrNull(),
            calories = etCalories.text.toString().toDoubleOrNull(),
            protein = etProtein.text.toString().toDoubleOrNull(),
            fat = etFat.text.toString().toDoubleOrNull()
        )

        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = apiService.createFood(foodData)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@AddFoodActivity,
                            "Корм успешно добавлен",
                            Toast.LENGTH_LONG
                        ).show()
                        val resultIntent = Intent().apply {
                            putExtra("BARCODE", foodData.barcode)
                            putExtra("FOOD_NAME", foodData.name)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        val errorMsg = response.body()?.message ?: "Ошибка при сохранении"
                        showError(errorMsg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Ошибка сети: ${e.message}")
                }
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateFood() {
        // Валидация
        if (etName.text.isNullOrBlank()) {
            etName.error = "Введите название корма"
            return
        }

        if (existingFoodId == null) {
            showError("Ошибка: ID корма не найден")
            return
        }

        val foodData = FoodUpdate(
            // Убираем id отсюда - он передается отдельно в URL
            name = etName.text.toString(),
            manufacturerId = manufacturers[spinnerManufacturer.selectedItemPosition].id,
            foodTypeId = foodTypes[spinnerFoodType.selectedItemPosition].id,
            flavorId = if (spinnerFlavor.selectedItemPosition > 0)
                flavors[spinnerFlavor.selectedItemPosition - 1].id else null,
            weight = etWeight.text.toString().toDoubleOrNull(),
            calories = etCalories.text.toString().toDoubleOrNull(),
            protein = etProtein.text.toString().toDoubleOrNull(),
            fat = etFat.text.toString().toDoubleOrNull()
        )

        showLoading(true)
        lifecycleScope.launch {
            try {
                // ID передается в URL
                val response = apiService.updateFood(existingFoodId!!, foodData)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@AddFoodActivity,
                            "Корм успешно обновлен",
                            Toast.LENGTH_LONG
                        ).show()
                        val resultIntent = Intent().apply {
                            putExtra("BARCODE", etBarcode.text.toString())
                            putExtra("FOOD_NAME", foodData.name)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        val errorMsg = response.body()?.message ?: "Ошибка при обновлении"
                        showError(errorMsg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Ошибка сети: ${e.message}")
                }
            } finally {
                showLoading(false)
            }
        }
    }
    private fun showAddManufacturerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val etName = dialogView.findViewById<EditText>(R.id.etItemName)
        val etCountry = dialogView.findViewById<EditText>(R.id.etExtraField)
        val tvExtraHint = dialogView.findViewById<TextView>(R.id.tvExtraHint)

        tvExtraHint.text = "Страна (необязательно)"
        etCountry.visibility = View.VISIBLE

        AlertDialog.Builder(this)
            .setTitle("Добавить производителя")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = etName.text.toString().trim()
                        if (name.isEmpty()) {
                            etName.error = "Введите название"
                            return@setOnClickListener
                        }

                        val country = etCountry.text.toString().trim()

                        showLoading(true)
                        lifecycleScope.launch {
                            try {
                                val newManufacturer = createManufacturer(name, country)
                                if (newManufacturer != null) {
                                    manufacturers.add(newManufacturer)
                                    setupManufacturerSpinner()
                                    spinnerManufacturer.setSelection(manufacturers.size - 1)
                                    Toast.makeText(
                                        this@AddFoodActivity,
                                        "Производитель добавлен",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dismiss()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@AddFoodActivity,
                                    "Ошибка: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                showLoading(false)
                            }
                        }
                    }
                }
                show()
            }
    }

    private suspend fun createManufacturer(name: String, country: String): Manufacturer? {
        return withContext(Dispatchers.IO) {
            try {
                val manufacturerData = mapOf(
                    "name" to name,
                    "country" to country
                )
                val response = apiService.createManufacturer(manufacturerData)
                if (response.isSuccessful && response.body()?.success == true) {
                    val id = response.body()?.data?.id
                    if (id != null) {
                        return@withContext Manufacturer(
                            id = id,
                            name = name,
                            country = country.ifEmpty { null }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    private fun showAddFlavorDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val etName = dialogView.findViewById<EditText>(R.id.etItemName)
        val etExtraField = dialogView.findViewById<EditText>(R.id.etExtraField)
        val tvExtraHint = dialogView.findViewById<TextView>(R.id.tvExtraHint)

        tvExtraHint.visibility = View.GONE
        etExtraField.visibility = View.GONE

        AlertDialog.Builder(this)
            .setTitle("Добавить вкус")
            .setView(dialogView)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = etName.text.toString().trim()
                        if (name.isEmpty()) {
                            etName.error = "Введите название"
                            return@setOnClickListener
                        }

                        showLoading(true)
                        lifecycleScope.launch {
                            try {
                                val newFlavor = createFlavor(name)
                                if (newFlavor != null) {
                                    flavors.add(newFlavor)
                                    setupFlavorSpinner()
                                    spinnerFlavor.setSelection(flavors.size)
                                    Toast.makeText(
                                        this@AddFoodActivity,
                                        "Вкус добавлен",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dismiss()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@AddFoodActivity,
                                    "Ошибка: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                showLoading(false)
                            }
                        }
                    }
                }
                show()
            }
    }

    private suspend fun createFlavor(name: String): Flavor? {
        return withContext(Dispatchers.IO) {
            try {
                val flavorData = mapOf("name" to name)
                val response = apiService.createFlavor(flavorData)
                if (response.isSuccessful && response.body()?.success == true) {
                    val id = response.body()?.data?.id
                    if (id != null) {
                        return@withContext Flavor(id = id, name = name)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled = !show
        btnScan.isEnabled = !show
        btnAddManufacturer.isEnabled = !show
        btnAddFlavor.isEnabled = !show
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}