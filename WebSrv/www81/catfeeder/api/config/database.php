<?php
class Database {
    private $host = "192.168.1.251";
    private $db_name = "cat_feeder";
    private $username = "cat_feeder_user";
    private $password = "FZkhe5dJZXmDZeMJtU";
    public $conn;
    
    public function getConnection() {
        $this->conn = null;
        
        try {
            $this->conn = new PDO(
                "mysql:host=" . $this->host . ";dbname=" . $this->db_name . ";charset=utf8mb4",
                $this->username,
                $this->password
            );
            $this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $this->conn->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        } catch(PDOException $e) {
            throw new Exception("Ошибка подключения к БД: " . $e->getMessage());
        }
        
        return $this->conn;
    }
}
?>
