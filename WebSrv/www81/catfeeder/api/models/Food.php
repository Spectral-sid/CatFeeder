<?php
class Food {
    private $conn;
    private $table = 'foods';
    
    public $id;
    public $barcode;
    public $name;
    public $manufacturer_id;
    public $food_type_id;
    public $flavor_id;
    public $weight_grams;
    public $protein_percent;
    public $fat_percent;
    public $fiber_percent;
    public $moisture_percent;
    public $ash_percent;
    public $calories;
    public $ingredients;
    public $photo_path;
    public $is_active;
    
    public function __construct($db) {
        $this->conn = $db;
    }
    
    // Получить корм по штрихкоду
    public function getByBarcode($barcode) {
        $query = "SELECT f.*, m.name as manufacturer_name, 
                         ft.name as type_name, ff.name as flavor_name
                  FROM " . $this->table . " f
                  JOIN manufacturers m ON f.manufacturer_id = m.id
                  JOIN food_types ft ON f.food_type_id = ft.id
                  LEFT JOIN food_flavors ff ON f.flavor_id = ff.id
                  WHERE f.barcode = :barcode AND f.is_active = TRUE
                  LIMIT 1";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':barcode', $barcode);
        $stmt->execute();
        
        return $stmt->fetch();
    }
    
    // Получить корм по ID
    public function getById($id) {
        $query = "SELECT f.*, m.name as manufacturer_name, 
                         ft.name as type_name, ff.name as flavor_name
                  FROM " . $this->table . " f
                  JOIN manufacturers m ON f.manufacturer_id = m.id
                  JOIN food_types ft ON f.food_type_id = ft.id
                  LEFT JOIN food_flavors ff ON f.flavor_id = ff.id
                  WHERE f.id = :id AND f.is_active = TRUE
                  LIMIT 1";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt->fetch();
    }
    
    // Создать новый корм
    public function create($data) {
        // Проверяем обязательные поля
        if (!isset($data['barcode']) || !isset($data['name'])) {
            return false;
        }
        
        $query = "INSERT INTO " . $this->table . " 
                  (barcode, name, manufacturer_id, food_type_id, flavor_id, 
                   weight_grams, protein_percent, fat_percent, fiber_percent,
                   moisture_percent, ash_percent, calories, ingredients,
                   photo_path, is_active, created_at)
                  VALUES 
                  (:barcode, :name, :manufacturer_id, :food_type_id, :flavor_id,
                   :weight_grams, :protein_percent, :fat_percent, :fiber_percent,
                   :moisture_percent, :ash_percent, :calories, :ingredients,
                   :photo_path, TRUE, NOW())";
        
        $stmt = $this->conn->prepare($query);
        
        // Устанавливаем значения по умолчанию
        $manufacturer_id = $data['manufacturerId'] ?? 1;
        $food_type_id = $data['foodTypeId'] ?? 1;
        $flavor_id = $data['flavorId'] ?? null;
        $weight_grams = $data['weight'] ?? $data['weight_grams'] ?? null;
        $protein_percent = $data['protein'] ?? $data['protein_percent'] ?? null;
        $fat_percent = $data['fat'] ?? $data['fat_percent'] ?? null;
        $fiber_percent = $data['fiber'] ?? $data['fiber_percent'] ?? null;
        $moisture_percent = $data['moisture'] ?? $data['moisture_percent'] ?? null;
        $ash_percent = $data['ash'] ?? $data['ash_percent'] ?? null;
        $calories = $data['calories'] ?? null;
        $ingredients = $data['ingredients'] ?? null;
        $photo_path = $data['photo'] ?? $data['photo_path'] ?? null;
        
        $stmt->bindParam(':barcode', $data['barcode']);
        $stmt->bindParam(':name', $data['name']);
        $stmt->bindParam(':manufacturer_id', $manufacturer_id, PDO::PARAM_INT);
        $stmt->bindParam(':food_type_id', $food_type_id, PDO::PARAM_INT);
        $stmt->bindParam(':flavor_id', $flavor_id, PDO::PARAM_INT);
        $stmt->bindParam(':weight_grams', $weight_grams);
        $stmt->bindParam(':protein_percent', $protein_percent);
        $stmt->bindParam(':fat_percent', $fat_percent);
        $stmt->bindParam(':fiber_percent', $fiber_percent);
        $stmt->bindParam(':moisture_percent', $moisture_percent);
        $stmt->bindParam(':ash_percent', $ash_percent);
        $stmt->bindParam(':calories', $calories);
        $stmt->bindParam(':ingredients', $ingredients);
        $stmt->bindParam(':photo_path', $photo_path);
        
        if ($stmt->execute()) {
            return $this->conn->lastInsertId();
        }
        
        return false;
    }
    
    // Обновить корм
    public function update($id, $data) {
        $query = "UPDATE " . $this->table . " 
                  SET name = :name,
                      manufacturer_id = :manufacturer_id,
                      food_type_id = :food_type_id,
                      flavor_id = :flavor_id,
                      weight_grams = :weight_grams,
                      protein_percent = :protein_percent,
                      fat_percent = :fat_percent,
                      fiber_percent = :fiber_percent,
                      moisture_percent = :moisture_percent,
                      ash_percent = :ash_percent,
                      calories = :calories,
                      ingredients = :ingredients,
                      photo_path = :photo_path,
                      updated_at = NOW()
                  WHERE id = :id";
        
        $stmt = $this->conn->prepare($query);
        
        $manufacturer_id = $data['manufacturer_id'] ?? 1;
        $food_type_id = $data['food_type_id'] ?? 1;
        $flavor_id = $data['flavor_id'] ?? null;
        $weight_grams = $data['weight'] ?? $data['weight_grams'] ?? null;
        $protein_percent = $data['protein'] ?? $data['protein_percent'] ?? null;
        $fat_percent = $data['fat'] ?? $data['fat_percent'] ?? null;
        $fiber_percent = $data['fiber'] ?? $data['fiber_percent'] ?? null;
        $moisture_percent = $data['moisture'] ?? $data['moisture_percent'] ?? null;
        $ash_percent = $data['ash'] ?? $data['ash_percent'] ?? null;
        $calories = $data['calories'] ?? null;
        $ingredients = $data['ingredients'] ?? null;
        $photo_path = $data['photo'] ?? $data['photo_path'] ?? null;
        
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        $stmt->bindParam(':name', $data['name']);
        $stmt->bindParam(':manufacturer_id', $manufacturer_id, PDO::PARAM_INT);
        $stmt->bindParam(':food_type_id', $food_type_id, PDO::PARAM_INT);
        $stmt->bindParam(':flavor_id', $flavor_id, PDO::PARAM_INT);
        $stmt->bindParam(':weight_grams', $weight_grams);
        $stmt->bindParam(':protein_percent', $protein_percent);
        $stmt->bindParam(':fat_percent', $fat_percent);
        $stmt->bindParam(':fiber_percent', $fiber_percent);
        $stmt->bindParam(':moisture_percent', $moisture_percent);
        $stmt->bindParam(':ash_percent', $ash_percent);
        $stmt->bindParam(':calories', $calories);
        $stmt->bindParam(':ingredients', $ingredients);
        $stmt->bindParam(':photo_path', $photo_path);
        
        return $stmt->execute();
    }
    
    // Получить список всех кормов
    public function getAll($limit = 100, $offset = 0) {
        $query = "SELECT f.*, m.name as manufacturer_name, ft.name as type_name 
                  FROM " . $this->table . " f
                  JOIN manufacturers m ON f.manufacturer_id = m.id
                  JOIN food_types ft ON f.food_type_id = ft.id
                  WHERE f.is_active = TRUE
                  ORDER BY f.name
                  LIMIT :limit OFFSET :offset";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
        $stmt->bindParam(':offset', $offset, PDO::PARAM_INT);
        $stmt->execute();
        
        return $stmt;
    }
    
    // Поиск кормов по названию
    public function search($searchTerm) {
        $query = "SELECT f.*, m.name as manufacturer_name, ft.name as type_name 
                  FROM " . $this->table . " f
                  JOIN manufacturers m ON f.manufacturer_id = m.id
                  JOIN food_types ft ON f.food_type_id = ft.id
                  WHERE f.is_active = TRUE 
                  AND (f.name LIKE :search OR f.barcode LIKE :search OR m.name LIKE :search)
                  ORDER BY f.name
                  LIMIT 50";
        
        $searchTerm = "%$searchTerm%";
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':search', $searchTerm);
        $stmt->execute();
        
        return $stmt;
    }
    
    // Удалить корм (мягкое удаление)
    public function delete($id) {
        $query = "UPDATE " . $this->table . " 
                  SET is_active = FALSE, updated_at = NOW() 
                  WHERE id = :id";
        
        $stmt = $this->conn->prepare($query);
        $stmt->bindParam(':id', $id, PDO::PARAM_INT);
        
        return $stmt->execute();
    }
}
?>
