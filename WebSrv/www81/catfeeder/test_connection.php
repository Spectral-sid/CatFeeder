<?php
require_once 'db_connect.php';

$conn = getDatabaseConnection();
if ($conn) {
    echo "✅ Подключение к базе данных успешно!";
    
    // Тестовый запрос
    $result = executeQuery($conn, "SELECT COUNT(*) as count FROM pets");
    $row = fetchOne($result);
    echo "<br>Количество питомцев в базе: " . $row['count'];
    
    closeConnection($conn);
} else {
    echo "❌ Ошибка подключения к базе данных";
}
?>
