CREATE DATABASE  IF NOT EXISTS `shuttle_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `shuttle_db`;
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: shuttle_db
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `boarding_log`
--

DROP TABLE IF EXISTS `boarding_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `boarding_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` bigint NOT NULL,
  `boarded_at` datetime DEFAULT NULL,
  `dropped_off_at` datetime DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `fk_log_booking` (`booking_id`),
  CONSTRAINT `fk_log_booking` FOREIGN KEY (`booking_id`) REFERENCES `trip_booking` (`booking_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boarding_log`
--

LOCK TABLES `boarding_log` WRITE;
/*!40000 ALTER TABLE `boarding_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `boarding_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `driver`
--

DROP TABLE IF EXISTS `driver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `driver` (
  `driver_id` bigint NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` varchar(255) NOT NULL,
  `is_verified` bit(1) NOT NULL,
  `join_date` date DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `total_trips` int NOT NULL,
  `student_number` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`driver_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `driver`
--

LOCK TABLES `driver` WRITE;
/*!40000 ALTER TABLE `driver` DISABLE KEYS */;
INSERT INTO `driver` VALUES (1,'Thabo','Nkosi','thabo.nkosi@shuttle.nmu.ac.za','0821234501','SHUTTLE_DRIVER',_binary '','2024-02-01','password123',142,NULL),(2,'Nomvula','Dube','nomvula.dube@shuttle.nmu.ac.za','0821234502','SHUTTLE_DRIVER',_binary '','2024-03-15','password123',98,NULL),(3,'Sipho','Mabaso','s223456789@mandela.ac.za','0731234503','STUDENT_DRIVER',_binary '','2025-01-20','password123',23,NULL),(4,'Aisha','Petersen','s223456790@mandela.ac.za','0731234504','STUDENT_DRIVER',_binary '','2025-02-10','password123',11,NULL),(5,'Luyanda','Zulu','s223456791@mandela.ac.za','0731234505','STUDENT_DRIVER',_binary '\0','2026-05-01','password123',0,NULL),(6,'Chloe','van der Merwe','s223456792@mandela.ac.za','0731234506','STUDENT_DRIVER',_binary '','2025-08-14','password123',7,NULL),(7,'Sam','Driver','sam.driver@example.com','0839876543','STUDENT_DRIVER',_binary '\0','2026-07-01','driverpass',0,NULL),(8,'John','Doe','john.doe@shuttle.com','0771122334','SHUTTLE_DRIVER',_binary '','2026-07-06','driver_pwd_1',50,NULL),(9,'Sarah','Wilson','sarah.w@shuttle.com','0775566778','STUDENT_DRIVER',_binary '','2026-07-06','driver_pwd_2',12,NULL);
/*!40000 ALTER TABLE `driver` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `driverapplications`
--

DROP TABLE IF EXISTS `driverapplications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `driverapplications` (
  `ApplicationID` bigint NOT NULL AUTO_INCREMENT,
  `driver_id` bigint NOT NULL,
  `contact_number` varchar(255) NOT NULL,
  `vehicle_make_model` varchar(255) NOT NULL,
  `registration_number` varchar(255) NOT NULL,
  `seating_capacity` int NOT NULL,
  `vehicle_color` varchar(255) NOT NULL,
  `license_image_path` varchar(255) NOT NULL DEFAULT '',
  `registration_file_path` varchar(255) NOT NULL DEFAULT '',
  `application_status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ApplicationID`),
  KEY `fk_application_driver` (`driver_id`),
  CONSTRAINT `fk_application_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`driver_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `driverapplications`
--

LOCK TABLES `driverapplications` WRITE;
/*!40000 ALTER TABLE `driverapplications` DISABLE KEYS */;
/*!40000 ALTER TABLE `driverapplications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `route`
--

DROP TABLE IF EXISTS `route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route` (
  `route_id` bigint NOT NULL AUTO_INCREMENT,
  `route_name` varchar(255) NOT NULL,
  `destination_stop_id` bigint NOT NULL,
  `origin_stop_id` bigint NOT NULL,
  PRIMARY KEY (`route_id`),
  KEY `FKpv8844e3m7ssdn9324fl65nhk` (`destination_stop_id`),
  KEY `FK6bctb2i1wky4p1ceimkk3u73d` (`origin_stop_id`),
  CONSTRAINT `FK6bctb2i1wky4p1ceimkk3u73d` FOREIGN KEY (`origin_stop_id`) REFERENCES `shuttle_stop` (`stop_id`),
  CONSTRAINT `FKpv8844e3m7ssdn9324fl65nhk` FOREIGN KEY (`destination_stop_id`) REFERENCES `shuttle_stop` (`stop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `route`
--

LOCK TABLES `route` WRITE;
/*!40000 ALTER TABLE `route` DISABLE KEYS */;
INSERT INTO `route` VALUES (1,'North Campus → South Campus',16,17),(2,'Korsten → North Campus',17,1),(3,'Summerstrand → Summerstrand Campus',18,14),(4,'Central → North Campus',17,8),(5,'North Campus ? South Campus',16,17),(6,'Korsten ? North Campus',17,1),(7,'Summerstrand ? Summerstrand Campus',18,14),(8,'Central ? North Campus',17,8),(9,'Korsten ↔ South Campus',16,1),(10,'Sydenham ↔ South Campus',16,2),(11,'Varsity Park ↔ South Campus',16,3),(12,'Richmond Hill ↔ South Campus',16,4),(13,'Russell Road ↔ South Campus',16,5),(14,'Feather Market Hall ↔ South Campus',16,6),(15,'Rink Street ↔ South Campus',16,7),(16,'Central ↔ South Campus',16,8),(17,'Walmer ↔ South Campus',16,9),(18,'Walmer Blvd ↔ South Campus',16,10),(19,'Humewood ↔ South Campus',16,11),(20,'Pier 14 ↔ South Campus',16,12),(21,'Forest Hill ↔ South Campus',16,13),(22,'Summerstrand ↔ South Campus',16,14),(23,'SSV Residences ↔ South Campus',16,15),(24,'Gomery Shuttle Stop ↔ South Campus',16,19),(25,'Korsten ↔ North Campus',17,1),(26,'Sydenham ↔ North Campus',17,2),(27,'Varsity Park ↔ North Campus',17,3),(28,'Richmond Hill ↔ North Campus',17,4),(29,'Russell Road ↔ North Campus',17,5),(30,'Feather Market Hall ↔ North Campus',17,6),(31,'Rink Street ↔ North Campus',17,7),(32,'Central ↔ North Campus',17,8),(33,'Walmer ↔ North Campus',17,9),(34,'Walmer Blvd ↔ North Campus',17,10),(35,'Humewood ↔ North Campus',17,11),(36,'Pier 14 ↔ North Campus',17,12),(37,'Forest Hill ↔ North Campus',17,13),(38,'Summerstrand ↔ North Campus',17,14),(39,'SSV Residences ↔ North Campus',17,15),(40,'Gomery Shuttle Stop ↔ North Campus',17,19);
/*!40000 ALTER TABLE `route` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shuttle_assignment`
--

DROP TABLE IF EXISTS `shuttle_assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shuttle_assignment` (
  `assignment_id` bigint NOT NULL AUTO_INCREMENT,
  `route_id` bigint NOT NULL,
  `driver_id` bigint NOT NULL,
  `registration_number` varchar(20) NOT NULL,
  `assignment_date` date NOT NULL,
  `shift_start` time NOT NULL,
  `shift_end` time NOT NULL,
  `current_direction` enum('OUTBOUND','INBOUND') DEFAULT 'OUTBOUND',
  `current_leg` int DEFAULT '1',
  `status` enum('SCHEDULED','RUNNING','BREAK','COMPLETED','CANCELLED') DEFAULT 'SCHEDULED',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  KEY `route_id` (`route_id`),
  KEY `driver_id` (`driver_id`),
  KEY `registration_number` (`registration_number`),
  CONSTRAINT `shuttle_assignment_ibfk_1` FOREIGN KEY (`route_id`) REFERENCES `route` (`route_id`),
  CONSTRAINT `shuttle_assignment_ibfk_2` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`driver_id`),
  CONSTRAINT `shuttle_assignment_ibfk_3` FOREIGN KEY (`registration_number`) REFERENCES `vehicle` (`registration_number`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shuttle_assignment`
--

LOCK TABLES `shuttle_assignment` WRITE;
/*!40000 ALTER TABLE `shuttle_assignment` DISABLE KEYS */;
/*!40000 ALTER TABLE `shuttle_assignment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shuttle_schedule`
--

DROP TABLE IF EXISTS `shuttle_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shuttle_schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT,
  `route_id` bigint NOT NULL,
  `slot_id` bigint NOT NULL,
  `available_seats` int DEFAULT '22',
  `price` decimal(10,2) DEFAULT '0.00',
  `is_active` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`schedule_id`),
  KEY `fk_schedule_route` (`route_id`),
  KEY `fk_schedule_slot` (`slot_id`),
  CONSTRAINT `fk_schedule_route` FOREIGN KEY (`route_id`) REFERENCES `route` (`route_id`),
  CONSTRAINT `fk_schedule_slot` FOREIGN KEY (`slot_id`) REFERENCES `shuttle_time_slot` (`slot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=321 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shuttle_schedule`
--

LOCK TABLES `shuttle_schedule` WRITE;
/*!40000 ALTER TABLE `shuttle_schedule` DISABLE KEYS */;
INSERT INTO `shuttle_schedule` VALUES (1,1,8,22,0.00,1),(2,1,7,22,0.00,1),(3,1,6,22,0.00,1),(4,1,5,22,0.00,1),(5,1,4,22,0.00,1),(6,1,3,22,0.00,1),(7,1,2,22,0.00,1),(8,1,1,22,0.00,1),(9,5,8,22,0.00,1),(10,5,7,22,0.00,1),(11,5,6,22,0.00,1),(12,5,5,22,0.00,1),(13,5,4,22,0.00,1),(14,5,3,22,0.00,1),(15,5,2,22,0.00,1),(16,5,1,22,0.00,1),(17,9,8,22,0.00,1),(18,9,7,22,0.00,1),(19,9,6,22,0.00,1),(20,9,5,22,0.00,1),(21,9,4,22,0.00,1),(22,9,3,22,0.00,1),(23,9,2,22,0.00,1),(24,9,1,22,0.00,1),(25,10,8,22,0.00,1),(26,10,7,22,0.00,1),(27,10,6,22,0.00,1),(28,10,5,22,0.00,1),(29,10,4,22,0.00,1),(30,10,3,22,0.00,1),(31,10,2,22,0.00,1),(32,10,1,22,0.00,1),(33,11,8,22,0.00,1),(34,11,7,22,0.00,1),(35,11,6,22,0.00,1),(36,11,5,22,0.00,1),(37,11,4,22,0.00,1),(38,11,3,22,0.00,1),(39,11,2,22,0.00,1),(40,11,1,22,0.00,1),(41,12,8,22,0.00,1),(42,12,7,22,0.00,1),(43,12,6,22,0.00,1),(44,12,5,22,0.00,1),(45,12,4,22,0.00,1),(46,12,3,22,0.00,1),(47,12,2,22,0.00,1),(48,12,1,22,0.00,1),(49,13,8,22,0.00,1),(50,13,7,22,0.00,1),(51,13,6,22,0.00,1),(52,13,5,22,0.00,1),(53,13,4,22,0.00,1),(54,13,3,22,0.00,1),(55,13,2,22,0.00,1),(56,13,1,22,0.00,1),(57,14,8,22,0.00,1),(58,14,7,22,0.00,1),(59,14,6,22,0.00,1),(60,14,5,22,0.00,1),(61,14,4,22,0.00,1),(62,14,3,22,0.00,1),(63,14,2,22,0.00,1),(64,14,1,22,0.00,1),(65,15,8,22,0.00,1),(66,15,7,22,0.00,1),(67,15,6,22,0.00,1),(68,15,5,22,0.00,1),(69,15,4,22,0.00,1),(70,15,3,22,0.00,1),(71,15,2,22,0.00,1),(72,15,1,22,0.00,1),(73,16,8,22,0.00,1),(74,16,7,22,0.00,1),(75,16,6,22,0.00,1),(76,16,5,22,0.00,1),(77,16,4,22,0.00,1),(78,16,3,22,0.00,1),(79,16,2,22,0.00,1),(80,16,1,22,0.00,1),(81,17,8,22,0.00,1),(82,17,7,22,0.00,1),(83,17,6,22,0.00,1),(84,17,5,22,0.00,1),(85,17,4,22,0.00,1),(86,17,3,22,0.00,1),(87,17,2,22,0.00,1),(88,17,1,22,0.00,1),(89,18,8,22,0.00,1),(90,18,7,22,0.00,1),(91,18,6,22,0.00,1),(92,18,5,22,0.00,1),(93,18,4,22,0.00,1),(94,18,3,22,0.00,1),(95,18,2,22,0.00,1),(96,18,1,22,0.00,1),(97,19,8,22,0.00,1),(98,19,7,22,0.00,1),(99,19,6,22,0.00,1),(100,19,5,22,0.00,1),(101,19,4,22,0.00,1),(102,19,3,22,0.00,1),(103,19,2,22,0.00,1),(104,19,1,22,0.00,1),(105,20,8,22,0.00,1),(106,20,7,22,0.00,1),(107,20,6,22,0.00,1),(108,20,5,22,0.00,1),(109,20,4,22,0.00,1),(110,20,3,22,0.00,1),(111,20,2,22,0.00,1),(112,20,1,22,0.00,1),(113,21,8,22,0.00,1),(114,21,7,22,0.00,1),(115,21,6,22,0.00,1),(116,21,5,22,0.00,1),(117,21,4,22,0.00,1),(118,21,3,22,0.00,1),(119,21,2,22,0.00,1),(120,21,1,22,0.00,1),(121,22,8,22,0.00,1),(122,22,7,22,0.00,1),(123,22,6,22,0.00,1),(124,22,5,22,0.00,1),(125,22,4,22,0.00,1),(126,22,3,22,0.00,1),(127,22,2,22,0.00,1),(128,22,1,22,0.00,1),(129,23,8,22,0.00,1),(130,23,7,22,0.00,1),(131,23,6,22,0.00,1),(132,23,5,22,0.00,1),(133,23,4,22,0.00,1),(134,23,3,22,0.00,1),(135,23,2,22,0.00,1),(136,23,1,22,0.00,1),(137,24,8,22,0.00,1),(138,24,7,22,0.00,1),(139,24,6,22,0.00,1),(140,24,5,22,0.00,1),(141,24,4,22,0.00,1),(142,24,3,22,0.00,1),(143,24,2,22,0.00,1),(144,24,1,22,0.00,1),(145,2,8,22,0.00,1),(146,2,7,22,0.00,1),(147,2,6,22,0.00,1),(148,2,5,22,0.00,1),(149,2,4,22,0.00,1),(150,2,3,22,0.00,1),(151,2,2,22,0.00,1),(152,2,1,22,0.00,1),(153,4,8,22,0.00,1),(154,4,7,22,0.00,1),(155,4,6,22,0.00,1),(156,4,5,22,0.00,1),(157,4,4,22,0.00,1),(158,4,3,22,0.00,1),(159,4,2,22,0.00,1),(160,4,1,22,0.00,1),(161,6,8,22,0.00,1),(162,6,7,22,0.00,1),(163,6,6,22,0.00,1),(164,6,5,22,0.00,1),(165,6,4,22,0.00,1),(166,6,3,22,0.00,1),(167,6,2,22,0.00,1),(168,6,1,22,0.00,1),(169,8,8,22,0.00,1),(170,8,7,22,0.00,1),(171,8,6,22,0.00,1),(172,8,5,22,0.00,1),(173,8,4,22,0.00,1),(174,8,3,22,0.00,1),(175,8,2,22,0.00,1),(176,8,1,22,0.00,1),(177,25,8,22,0.00,1),(178,25,7,22,0.00,1),(179,25,6,22,0.00,1),(180,25,5,22,0.00,1),(181,25,4,22,0.00,1),(182,25,3,22,0.00,1),(183,25,2,22,0.00,1),(184,25,1,22,0.00,1),(185,26,8,22,0.00,1),(186,26,7,22,0.00,1),(187,26,6,22,0.00,1),(188,26,5,22,0.00,1),(189,26,4,22,0.00,1),(190,26,3,22,0.00,1),(191,26,2,22,0.00,1),(192,26,1,22,0.00,1),(193,27,8,22,0.00,1),(194,27,7,22,0.00,1),(195,27,6,22,0.00,1),(196,27,5,22,0.00,1),(197,27,4,22,0.00,1),(198,27,3,22,0.00,1),(199,27,2,22,0.00,1),(200,27,1,22,0.00,1),(201,28,8,22,0.00,1),(202,28,7,22,0.00,1),(203,28,6,22,0.00,1),(204,28,5,22,0.00,1),(205,28,4,22,0.00,1),(206,28,3,22,0.00,1),(207,28,2,22,0.00,1),(208,28,1,22,0.00,1),(209,29,8,22,0.00,1),(210,29,7,22,0.00,1),(211,29,6,22,0.00,1),(212,29,5,22,0.00,1),(213,29,4,22,0.00,1),(214,29,3,22,0.00,1),(215,29,2,22,0.00,1),(216,29,1,22,0.00,1),(217,30,8,22,0.00,1),(218,30,7,22,0.00,1),(219,30,6,22,0.00,1),(220,30,5,22,0.00,1),(221,30,4,22,0.00,1),(222,30,3,22,0.00,1),(223,30,2,22,0.00,1),(224,30,1,22,0.00,1),(225,31,8,22,0.00,1),(226,31,7,22,0.00,1),(227,31,6,22,0.00,1),(228,31,5,22,0.00,1),(229,31,4,22,0.00,1),(230,31,3,22,0.00,1),(231,31,2,22,0.00,1),(232,31,1,22,0.00,1),(233,32,8,22,0.00,1),(234,32,7,22,0.00,1),(235,32,6,22,0.00,1),(236,32,5,22,0.00,1),(237,32,4,22,0.00,1),(238,32,3,22,0.00,1),(239,32,2,22,0.00,1),(240,32,1,22,0.00,1),(241,33,8,22,0.00,1),(242,33,7,22,0.00,1),(243,33,6,22,0.00,1),(244,33,5,22,0.00,1),(245,33,4,22,0.00,1),(246,33,3,22,0.00,1),(247,33,2,22,0.00,1),(248,33,1,22,0.00,1),(249,34,8,22,0.00,1),(250,34,7,22,0.00,1),(251,34,6,22,0.00,1),(252,34,5,22,0.00,1),(253,34,4,22,0.00,1),(254,34,3,22,0.00,1),(255,34,2,22,0.00,1),(256,34,1,22,0.00,1),(257,35,8,22,0.00,1),(258,35,7,22,0.00,1),(259,35,6,22,0.00,1),(260,35,5,22,0.00,1),(261,35,4,22,0.00,1),(262,35,3,22,0.00,1),(263,35,2,22,0.00,1),(264,35,1,22,0.00,1),(265,36,8,22,0.00,1),(266,36,7,22,0.00,1),(267,36,6,22,0.00,1),(268,36,5,22,0.00,1),(269,36,4,22,0.00,1),(270,36,3,22,0.00,1),(271,36,2,22,0.00,1),(272,36,1,22,0.00,1),(273,37,8,22,0.00,1),(274,37,7,22,0.00,1),(275,37,6,22,0.00,1),(276,37,5,22,0.00,1),(277,37,4,22,0.00,1),(278,37,3,22,0.00,1),(279,37,2,22,0.00,1),(280,37,1,22,0.00,1),(281,38,8,22,0.00,1),(282,38,7,22,0.00,1),(283,38,6,22,0.00,1),(284,38,5,22,0.00,1),(285,38,4,22,0.00,1),(286,38,3,22,0.00,1),(287,38,2,22,0.00,1),(288,38,1,22,0.00,1),(289,39,8,22,0.00,1),(290,39,7,22,0.00,1),(291,39,6,22,0.00,1),(292,39,5,22,0.00,1),(293,39,4,22,0.00,1),(294,39,3,22,0.00,1),(295,39,2,22,0.00,1),(296,39,1,22,0.00,1),(297,40,8,22,0.00,1),(298,40,7,22,0.00,1),(299,40,6,22,0.00,1),(300,40,5,22,0.00,1),(301,40,4,22,0.00,1),(302,40,3,22,0.00,1),(303,40,2,22,0.00,1),(304,40,1,22,0.00,1),(305,3,8,22,0.00,1),(306,3,7,22,0.00,1),(307,3,6,22,0.00,1),(308,3,5,22,0.00,1),(309,3,4,22,0.00,1),(310,3,3,22,0.00,1),(311,3,2,22,0.00,1),(312,3,1,22,0.00,1),(313,7,8,22,0.00,1),(314,7,7,22,0.00,1),(315,7,6,22,0.00,1),(316,7,5,22,0.00,1),(317,7,4,22,0.00,1),(318,7,3,22,0.00,1),(319,7,2,22,0.00,1),(320,7,1,22,0.00,1);
/*!40000 ALTER TABLE `shuttle_schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shuttle_stop`
--

DROP TABLE IF EXISTS `shuttle_stop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shuttle_stop` (
  `stop_id` bigint NOT NULL AUTO_INCREMENT,
  `stop_name` varchar(255) NOT NULL,
  `area` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  PRIMARY KEY (`stop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shuttle_stop`
--

LOCK TABLES `shuttle_stop` WRITE;
/*!40000 ALTER TABLE `shuttle_stop` DISABLE KEYS */;
INSERT INTO `shuttle_stop` VALUES (1,'Korsten','Korsten','PSA, 163 Durban Rd',NULL,NULL),(2,'Sydenham','Korsten','Klesal / PSA, 10 on Smart',NULL,NULL),(3,'Varsity Park','Central','Law Court (Smada)',NULL,NULL),(4,'Richmond Hill','Central','Kalinga House',NULL,NULL),(5,'Russell Road','Central','Home Choice',NULL,NULL),(6,'Feather Market Hall','Central','Govan Mbeki Ave',NULL,NULL),(7,'Rink Street','Central','The Suites',NULL,NULL),(8,'Central','Central','Belmont Terrace',NULL,NULL),(9,'Walmer','Walmer','PSA',NULL,NULL),(10,'Walmer Blvd','Walmer','Shell Garage',NULL,NULL),(11,'Humewood','Humewood','Kings Beach',NULL,NULL),(12,'Pier 14','Pier 14','Pier 14',NULL,NULL),(13,'Forest Hill','Forest Hill','Garage, Morestond Flats and Stadium',NULL,NULL),(14,'Summerstrand','Summerstrand','Summerbreeze Spar',NULL,NULL),(15,'SSSV Residences','Summerstrand','Summerstrand',NULL,NULL),(16,'South Campus','Summerstrand','Summerstrand',NULL,NULL),(17,'North Campus','Summerstrand','Summerstrand',NULL,NULL),(18,'Summerstrand Campus','Summerstrand','Campus Pick n Pay',NULL,NULL),(19,'Gomery Shuttle Stop','Summerstrand','Gomery Place / Omega / Dunes',NULL,NULL);
/*!40000 ALTER TABLE `shuttle_stop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shuttle_time_slot`
--

DROP TABLE IF EXISTS `shuttle_time_slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shuttle_time_slot` (
  `slot_id` bigint NOT NULL AUTO_INCREMENT,
  `period` enum('Morning','Afternoon') NOT NULL,
  `departs` time NOT NULL,
  `arrives` time NOT NULL,
  PRIMARY KEY (`slot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shuttle_time_slot`
--

LOCK TABLES `shuttle_time_slot` WRITE;
/*!40000 ALTER TABLE `shuttle_time_slot` DISABLE KEYS */;
INSERT INTO `shuttle_time_slot` VALUES (1,'Morning','06:45:00','07:30:00'),(2,'Morning','07:45:00','08:30:00'),(3,'Morning','08:45:00','09:30:00'),(4,'Morning','09:45:00','10:30:00'),(5,'Afternoon','12:30:00','13:15:00'),(6,'Afternoon','14:30:00','15:15:00'),(7,'Afternoon','16:00:00','16:45:00'),(8,'Afternoon','17:30:00','18:15:00');
/*!40000 ALTER TABLE `shuttle_time_slot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `student_id` bigint NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `student_number` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_funded` bit(1) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`student_id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `student_number` (`student_number`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student`
--

LOCK TABLES `student` WRITE;
/*!40000 ALTER TABLE `student` DISABLE KEYS */;
INSERT INTO `student` VALUES (1,'Kelvin','Mudzingwa','test@mandela.ac.za','0821234567','S12345678','2026-06-30 17:11:28.998438',_binary '\0','test123'),(2,'ghon','ktwl','testinge@mandela.ac.za','','2250418','2026-07-01 13:13:10.021605',_binary '\0','rxbjytv'),(3,'Alice','Johnson','alice.j@example.edu','0123456789','20210001','2026-07-06 16:33:37.000000',_binary '','hashed_pwd_1'),(4,'Bob','Smith','bob.s@example.edu','0987654321','20210002','2026-07-06 16:33:37.000000',_binary '\0','hashed_pwd_2'),(5,'Charlie','Davis','charlie.d@example.edu','0112233445','20210003','2026-07-06 16:33:37.000000',_binary '','hashed_pwd_3'),(6,'student','m','nsfas@mandela.ac.za','','22501962','2026-07-16 18:34:22.509446',_binary '\0','testing123'),(7,'test','test','nsfas1@mandela.ac.za','','1284069','2026-07-18 22:49:26.838489',_binary '\0','1234557'),(8,'test','2','testionhg@mandela.ac.za','','49046','2026-07-18 22:58:03.311934',_binary '\0','tbsjbs'),(9,'the','test','hel@mandela.ac.za','','28013','2026-07-18 23:02:19.625642',_binary '\0','qtgkr'),(10,'youn','lee','younglee@mandela.ac.za','','28053','2026-07-18 23:10:02.284370',_binary '','test123'),(13,'Kelvin','Mudzingwa','test1@mandela.ac.za','0821234567','S123456','2026-08-01 07:37:14.795148',_binary '\0','$2a$10$kVnHSoUktju12.0Kfgkjyu096B5VSAwAWDpqGdUe08mUVUTtUmyBa'),(14,'Boarding','Test','boarding.test@mandela.ac.za','0812345678','20260001','2026-08-01 12:40:10.941517',_binary '','$2a$10$QR9Fv1NxPGKYZ61HBuVi7OSoIxDFCpobChxch/qZ9.E3UgVv0YbtS'),(15,'kelvin','Mudzingwa','kelvin@mandela.ac.za','','123456','2026-08-03 15:07:06.419997',_binary '','$2a$10$C40uPxDNe91wFCN3q9mDs.zo8ALlMU/x1hXobVCJHb1/nfYSX4NOq');
/*!40000 ALTER TABLE `student` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trip`
--

DROP TABLE IF EXISTS `trip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trip` (
  `trip_id` bigint NOT NULL AUTO_INCREMENT,
  `driver_id` bigint NOT NULL,
  `registration_number` varchar(20) NOT NULL,
  `trip_type` varchar(255) NOT NULL,
  `slot_id` bigint DEFAULT NULL,
  `departure_stop` varchar(255) NOT NULL,
  `destination_stop` varchar(255) NOT NULL,
  `departure_time` datetime DEFAULT NULL,
  `arrival_time` datetime DEFAULT NULL,
  `available_seats` int DEFAULT NULL,
  `price` decimal(8,2) DEFAULT '0.00',
  `status` varchar(255) NOT NULL,
  `departure_lat` double DEFAULT NULL,
  `departure_lng` double DEFAULT NULL,
  `destination_lat` double DEFAULT NULL,
  `destination_lng` double DEFAULT NULL,
  `current_lat` double DEFAULT NULL,
  `current_lng` double DEFAULT NULL,
  `current_leg_index` int DEFAULT '0',
  `current_point_index` int DEFAULT '0',
  `dwell_until` datetime DEFAULT NULL,
  `route_id` bigint DEFAULT NULL,
  PRIMARY KEY (`trip_id`),
  KEY `fk_trip_driver` (`driver_id`),
  KEY `fk_trip_vehicle` (`registration_number`),
  KEY `fk_trip_slot` (`slot_id`),
  KEY `FKeva4adpyk6glllffnw5ypj20j` (`route_id`),
  CONSTRAINT `fk_trip_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`driver_id`),
  CONSTRAINT `fk_trip_slot` FOREIGN KEY (`slot_id`) REFERENCES `shuttle_time_slot` (`slot_id`),
  CONSTRAINT `fk_trip_vehicle` FOREIGN KEY (`registration_number`) REFERENCES `vehicle` (`registration_number`),
  CONSTRAINT `FKeva4adpyk6glllffnw5ypj20j` FOREIGN KEY (`route_id`) REFERENCES `route` (`route_id`)
) ENGINE=InnoDB AUTO_INCREMENT=558 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trip`
--

LOCK TABLES `trip` WRITE;
/*!40000 ALTER TABLE `trip` DISABLE KEYS */;
INSERT INTO `trip` VALUES (1,3,'CA123456','Carpool',NULL,'Kwazakhele, Ngxabane Street','South Campus','2026-07-01 07:15:00','2026-07-06 16:38:57',3,25.00,'COMPLETED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(2,3,'CA123456','Carpool',NULL,'South Campus','Kwazakhele, Ngxabane Street','2026-07-01 17:00:00',NULL,3,25.00,'CANCELLED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(3,4,'CA654321','Carpool',NULL,'Newton Park, Cape Road Spar','2nd Avenue Campus','2026-07-01 08:00:00',NULL,0,30.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(4,4,'CA654321','Carpool',NULL,'Newton Park, Cape Road Spar','North Campus','2026-06-29 08:00:00','2026-06-29 08:25:00',2,30.00,'COMPLETED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(5,6,'CA999888','Carpool',NULL,'Summerstrand, Marine Drive','South Campus','2026-06-30 07:30:00',NULL,3,20.00,'CANCELLED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(6,6,'CA999888','Carpool',NULL,'Walmer, 6th Avenue','South Campus','2026-07-02 07:00:00',NULL,1,20.00,'CANCELLED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(7,7,'CB123456','STUDENT_DRIVER',NULL,'Bird Street, Gqeberha','Summerstrand, Gqeberha','2026-07-01 14:00:00',NULL,2,30.00,'CONFIRMED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(13,7,'CB123456','STUDENT_DRIVER',NULL,'Bird Street, Gqeberha','Summerstrand, Gqeberha','2026-07-01 14:00:00',NULL,2,30.00,'CONFIRMED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(14,7,'CB123456','STUDENT_DRIVER',NULL,'Bird Street, Gqeberha','Summerstrand, Gqeberha','2026-07-01 14:00:00',NULL,2,30.00,'CONFIRMED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(20,7,'CB123456','STUDENT_DRIVER',NULL,'Bird Street, Gqeberha','Summerstrand, Gqeberha','2026-07-01 14:00:00',NULL,2,30.00,'CONFIRMED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,NULL),(21,7,'CB123456','STUDENT_DRIVER',NULL,'Bird Street, Gqeberha','Summerstrand, Gqeberha','2026-07-01 14:00:00',NULL,2,30.00,'CONFIRMED',-33.9581,25.6011,-33.9997,25.6698,NULL,NULL,0,0,NULL,NULL),(22,7,'CB123456','Carpool',NULL,'4GR72J96+C4 Walmer, Gqeberha','4GR72M29+CX Summerstrand, Gqeberha','2026-07-01 14:00:00',NULL,2,30.00,'CONFIRMED',-33.9814,25.6103,-33.9989,25.6699,NULL,NULL,0,0,NULL,NULL),(23,7,'CB123456','Carpool',NULL,'4GR72J96+C4 Walmer, Gqeberha','4GR72M29+CX Summerstrand, Gqeberha','2026-07-01 14:00:00',NULL,2,30.00,'CONFIRMED',-33.9814,25.6103,-33.9989,25.6699,NULL,NULL,0,0,NULL,NULL),(24,1,'ABC 123 EC','SHUTTLE',NULL,'North Campus','South Campus','2026-07-06 17:33:37',NULL,1,0.00,'SCHEDULED',-33.9912,25.6698,-33.9984,25.675,NULL,NULL,0,0,NULL,NULL),(25,2,'XYZ 789 EC','PRIVATE',NULL,'Summerstrand','Missionvale','2026-07-06 18:33:37',NULL,0,15.50,'SCHEDULED',-34.0021,25.6601,-33.91,25.55,NULL,NULL,0,0,NULL,NULL),(26,1,'ABC 123 EC','SHUTTLE',6,'North Campus','South Campus','2026-08-03 14:30:00',NULL,5,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(27,2,'XYZ 789 EC','SHUTTLE',6,'North Campus','South Campus','2026-08-03 14:30:00',NULL,0,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(28,1,'NMU001EC','SHUTTLE',1,'Korsten','North Campus','2026-08-03 06:45:00',NULL,12,0.00,'CONFIRMED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(29,2,'NMU002EC','SHUTTLE',6,'Central','North Campus','2026-08-03 14:30:00',NULL,0,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(30,1,'ABC 123 EC','SHUTTLE',6,'North Campus','South Campus','2026-08-03 14:30:00',NULL,5,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(31,1,'ABC 123 EC','SHUTTLE',6,'North Campus','South Campus','2026-08-03 14:30:00',NULL,5,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(33,2,'XYZ 789 EC','SHUTTLE',6,'North Campus','South Campus','2026-08-03 14:30:00',NULL,0,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(34,2,'XYZ 789 EC','SHUTTLE',6,'North Campus','South Campus','2026-08-03 14:30:00',NULL,0,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(36,1,'NMU001EC','SHUTTLE',1,'Korsten','North Campus','2026-08-03 06:45:00',NULL,12,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(37,1,'NMU001EC','SHUTTLE',1,'Korsten','North Campus','2026-08-03 06:45:00',NULL,12,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(39,2,'NMU002EC','SHUTTLE',6,'Central','North Campus','2026-08-03 14:30:00',NULL,0,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(40,2,'NMU002EC','SHUTTLE',6,'Central','North Campus','2026-08-03 14:30:00',NULL,0,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(41,1,'NMU001EC','SHUTTLE',5,'Summerstrand','Summerstrand Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,7),(42,1,'NMU001EC','SHUTTLE',2,'Summerstrand','Summerstrand Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,3),(43,1,'NMU001EC','SHUTTLE',7,'Summerstrand','Summerstrand Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,3),(44,1,'NMU001EC','SHUTTLE',4,'Gomery Shuttle Stop','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,40),(45,1,'NMU001EC','SHUTTLE',1,'SSSV Residences','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,39),(46,1,'NMU001EC','SHUTTLE',6,'SSSV Residences','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,39),(47,1,'NMU001EC','SHUTTLE',3,'Summerstrand','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,38),(48,1,'NMU001EC','SHUTTLE',8,'Summerstrand','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,38),(49,1,'NMU001EC','SHUTTLE',5,'Forest Hill','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,37),(50,1,'NMU001EC','SHUTTLE',2,'Pier 14','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',21,0.00,'CONFIRMED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,36),(51,1,'NMU001EC','SHUTTLE',7,'Pier 14','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,36),(52,1,'NMU001EC','SHUTTLE',4,'Humewood','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,35),(53,1,'NMU001EC','SHUTTLE',1,'Walmer Blvd','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,34),(54,1,'NMU001EC','SHUTTLE',6,'Walmer Blvd','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,34),(55,1,'NMU001EC','SHUTTLE',3,'Walmer','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,33),(56,1,'NMU001EC','SHUTTLE',8,'Walmer','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,33),(57,1,'NMU001EC','SHUTTLE',5,'Central','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,32),(58,1,'NMU001EC','SHUTTLE',2,'Rink Street','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,31),(59,1,'NMU001EC','SHUTTLE',7,'Rink Street','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,31),(60,1,'NMU001EC','SHUTTLE',4,'Feather Market Hall','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,30),(61,1,'NMU001EC','SHUTTLE',1,'Russell Road','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,29),(62,1,'NMU001EC','SHUTTLE',6,'Russell Road','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,29),(63,1,'NMU001EC','SHUTTLE',3,'Richmond Hill','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,28),(64,1,'NMU001EC','SHUTTLE',8,'Richmond Hill','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,28),(65,1,'NMU001EC','SHUTTLE',5,'Varsity Park','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,27),(66,1,'NMU001EC','SHUTTLE',2,'Sydenham','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,26),(67,1,'NMU001EC','SHUTTLE',7,'Sydenham','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,26),(68,1,'NMU001EC','SHUTTLE',4,'Korsten','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,25),(69,1,'NMU001EC','SHUTTLE',1,'Central','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(70,1,'NMU001EC','SHUTTLE',6,'Central','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(71,1,'NMU001EC','SHUTTLE',3,'Korsten','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(72,1,'NMU001EC','SHUTTLE',8,'Korsten','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(73,1,'NMU001EC','SHUTTLE',5,'Central','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(74,1,'NMU001EC','SHUTTLE',2,'Korsten','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(75,1,'NMU001EC','SHUTTLE',7,'Korsten','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(76,1,'NMU001EC','SHUTTLE',4,'Gomery Shuttle Stop','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,24),(77,1,'NMU001EC','SHUTTLE',1,'SSSV Residences','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,23),(78,1,'NMU001EC','SHUTTLE',6,'SSSV Residences','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,23),(79,1,'NMU001EC','SHUTTLE',3,'Summerstrand','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,22),(80,1,'NMU001EC','SHUTTLE',8,'Summerstrand','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,22),(81,1,'NMU001EC','SHUTTLE',5,'Forest Hill','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,21),(82,1,'NMU001EC','SHUTTLE',2,'Pier 14','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,20),(83,1,'NMU001EC','SHUTTLE',7,'Pier 14','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,20),(84,1,'NMU001EC','SHUTTLE',4,'Humewood','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,19),(85,1,'NMU001EC','SHUTTLE',1,'Walmer Blvd','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,18),(86,1,'NMU001EC','SHUTTLE',6,'Walmer Blvd','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,18),(87,1,'NMU001EC','SHUTTLE',3,'Walmer','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,17),(88,1,'NMU001EC','SHUTTLE',8,'Walmer','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,17),(89,1,'NMU001EC','SHUTTLE',5,'Central','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,16),(90,1,'NMU001EC','SHUTTLE',2,'Rink Street','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,15),(91,1,'NMU001EC','SHUTTLE',7,'Rink Street','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,15),(92,1,'NMU001EC','SHUTTLE',4,'Feather Market Hall','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,14),(93,1,'NMU001EC','SHUTTLE',1,'Russell Road','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,13),(94,1,'NMU001EC','SHUTTLE',6,'Russell Road','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,13),(95,1,'NMU001EC','SHUTTLE',3,'Richmond Hill','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,12),(96,1,'NMU001EC','SHUTTLE',8,'Richmond Hill','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,12),(97,1,'NMU001EC','SHUTTLE',5,'Varsity Park','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,11),(98,1,'NMU001EC','SHUTTLE',2,'Sydenham','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,10),(99,1,'NMU001EC','SHUTTLE',7,'Sydenham','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,10),(100,1,'NMU001EC','SHUTTLE',4,'Korsten','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,9),(101,1,'NMU001EC','SHUTTLE',1,'North Campus','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(102,1,'NMU001EC','SHUTTLE',6,'North Campus','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(103,1,'NMU001EC','SHUTTLE',3,'North Campus','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(104,1,'NMU001EC','SHUTTLE',8,'North Campus','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(105,2,'NMU002EC','SHUTTLE',4,'Summerstrand','Summerstrand Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,7),(106,2,'NMU002EC','SHUTTLE',1,'Summerstrand','Summerstrand Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,3),(107,2,'NMU002EC','SHUTTLE',6,'Summerstrand','Summerstrand Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,3),(108,2,'NMU002EC','SHUTTLE',3,'Gomery Shuttle Stop','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,40),(109,2,'NMU002EC','SHUTTLE',8,'Gomery Shuttle Stop','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,40),(110,2,'NMU002EC','SHUTTLE',5,'SSSV Residences','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,39),(111,2,'NMU002EC','SHUTTLE',2,'Summerstrand','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,38),(112,2,'NMU002EC','SHUTTLE',7,'Summerstrand','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,38),(113,2,'NMU002EC','SHUTTLE',4,'Forest Hill','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,37),(114,2,'NMU002EC','SHUTTLE',1,'Pier 14','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,36),(115,2,'NMU002EC','SHUTTLE',6,'Pier 14','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,36),(116,2,'NMU002EC','SHUTTLE',3,'Humewood','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,35),(117,2,'NMU002EC','SHUTTLE',8,'Humewood','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,35),(118,2,'NMU002EC','SHUTTLE',5,'Walmer Blvd','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,34),(119,2,'NMU002EC','SHUTTLE',2,'Walmer','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,33),(120,2,'NMU002EC','SHUTTLE',7,'Walmer','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,33),(121,2,'NMU002EC','SHUTTLE',4,'Central','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,32),(122,2,'NMU002EC','SHUTTLE',1,'Rink Street','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,31),(123,2,'NMU002EC','SHUTTLE',6,'Rink Street','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,31),(124,2,'NMU002EC','SHUTTLE',3,'Feather Market Hall','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,30),(125,2,'NMU002EC','SHUTTLE',8,'Feather Market Hall','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,30),(126,2,'NMU002EC','SHUTTLE',5,'Russell Road','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,29),(127,2,'NMU002EC','SHUTTLE',2,'Richmond Hill','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,28),(128,2,'NMU002EC','SHUTTLE',7,'Richmond Hill','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,28),(129,2,'NMU002EC','SHUTTLE',4,'Varsity Park','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,27),(130,2,'NMU002EC','SHUTTLE',1,'Sydenham','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,26),(131,2,'NMU002EC','SHUTTLE',6,'Sydenham','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,26),(132,2,'NMU002EC','SHUTTLE',3,'Korsten','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,25),(133,2,'NMU002EC','SHUTTLE',8,'Korsten','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,25),(134,2,'NMU002EC','SHUTTLE',5,'Central','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(135,2,'NMU002EC','SHUTTLE',2,'Korsten','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(136,2,'NMU002EC','SHUTTLE',7,'Korsten','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(137,2,'NMU002EC','SHUTTLE',4,'Central','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(138,2,'NMU002EC','SHUTTLE',1,'Korsten','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(139,2,'NMU002EC','SHUTTLE',6,'Korsten','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(140,2,'NMU002EC','SHUTTLE',3,'Gomery Shuttle Stop','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,24),(141,2,'NMU002EC','SHUTTLE',8,'Gomery Shuttle Stop','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,24),(142,2,'NMU002EC','SHUTTLE',5,'SSSV Residences','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,23),(143,2,'NMU002EC','SHUTTLE',2,'Summerstrand','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,22),(144,2,'NMU002EC','SHUTTLE',7,'Summerstrand','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,22),(145,2,'NMU002EC','SHUTTLE',4,'Forest Hill','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,21),(146,2,'NMU002EC','SHUTTLE',1,'Pier 14','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,20),(147,2,'NMU002EC','SHUTTLE',6,'Pier 14','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,20),(148,2,'NMU002EC','SHUTTLE',3,'Humewood','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,19),(149,2,'NMU002EC','SHUTTLE',8,'Humewood','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,19),(150,2,'NMU002EC','SHUTTLE',5,'Walmer Blvd','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,18),(151,2,'NMU002EC','SHUTTLE',2,'Walmer','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,17),(152,2,'NMU002EC','SHUTTLE',7,'Walmer','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,17),(153,2,'NMU002EC','SHUTTLE',4,'Central','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,16),(154,2,'NMU002EC','SHUTTLE',1,'Rink Street','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,15),(155,2,'NMU002EC','SHUTTLE',6,'Rink Street','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,15),(156,2,'NMU002EC','SHUTTLE',3,'Feather Market Hall','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,14),(157,2,'NMU002EC','SHUTTLE',8,'Feather Market Hall','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,14),(158,2,'NMU002EC','SHUTTLE',5,'Russell Road','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,13),(159,2,'NMU002EC','SHUTTLE',2,'Richmond Hill','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,12),(160,2,'NMU002EC','SHUTTLE',7,'Richmond Hill','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,12),(161,2,'NMU002EC','SHUTTLE',4,'Varsity Park','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,11),(162,2,'NMU002EC','SHUTTLE',1,'Sydenham','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,10),(163,2,'NMU002EC','SHUTTLE',6,'Sydenham','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,10),(164,2,'NMU002EC','SHUTTLE',3,'Korsten','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,9),(165,2,'NMU002EC','SHUTTLE',8,'Korsten','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,9),(166,2,'NMU002EC','SHUTTLE',5,'North Campus','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(167,2,'NMU002EC','SHUTTLE',2,'North Campus','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(168,2,'NMU002EC','SHUTTLE',7,'North Campus','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(169,1,'ABC 123 EC','SHUTTLE',3,'Summerstrand','Summerstrand Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,7),(170,1,'ABC 123 EC','SHUTTLE',8,'Summerstrand','Summerstrand Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,7),(171,1,'ABC 123 EC','SHUTTLE',5,'Summerstrand','Summerstrand Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,3),(172,1,'ABC 123 EC','SHUTTLE',2,'Gomery Shuttle Stop','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,40),(173,1,'ABC 123 EC','SHUTTLE',7,'Gomery Shuttle Stop','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,40),(174,1,'ABC 123 EC','SHUTTLE',4,'SSSV Residences','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,39),(175,1,'ABC 123 EC','SHUTTLE',1,'Summerstrand','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,38),(176,1,'ABC 123 EC','SHUTTLE',6,'Summerstrand','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,38),(177,1,'ABC 123 EC','SHUTTLE',3,'Forest Hill','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,37),(178,1,'ABC 123 EC','SHUTTLE',8,'Forest Hill','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,37),(179,1,'ABC 123 EC','SHUTTLE',5,'Pier 14','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,36),(180,1,'ABC 123 EC','SHUTTLE',2,'Humewood','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,35),(181,1,'ABC 123 EC','SHUTTLE',7,'Humewood','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,35),(182,1,'ABC 123 EC','SHUTTLE',4,'Walmer Blvd','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,34),(183,1,'ABC 123 EC','SHUTTLE',1,'Walmer','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,33),(184,1,'ABC 123 EC','SHUTTLE',6,'Walmer','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,33),(185,1,'ABC 123 EC','SHUTTLE',3,'Central','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,32),(186,1,'ABC 123 EC','SHUTTLE',8,'Central','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,32),(187,1,'ABC 123 EC','SHUTTLE',5,'Rink Street','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,31),(188,1,'ABC 123 EC','SHUTTLE',2,'Feather Market Hall','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,30),(189,1,'ABC 123 EC','SHUTTLE',7,'Feather Market Hall','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,30),(190,1,'ABC 123 EC','SHUTTLE',4,'Russell Road','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,29),(191,1,'ABC 123 EC','SHUTTLE',1,'Richmond Hill','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,28),(192,1,'ABC 123 EC','SHUTTLE',6,'Richmond Hill','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,28),(193,1,'ABC 123 EC','SHUTTLE',3,'Varsity Park','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,27),(194,1,'ABC 123 EC','SHUTTLE',8,'Varsity Park','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,27),(195,1,'ABC 123 EC','SHUTTLE',5,'Sydenham','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,26),(196,1,'ABC 123 EC','SHUTTLE',2,'Korsten','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,25),(197,1,'ABC 123 EC','SHUTTLE',7,'Korsten','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,25),(198,1,'ABC 123 EC','SHUTTLE',4,'Central','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(199,1,'ABC 123 EC','SHUTTLE',1,'Korsten','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(200,1,'ABC 123 EC','SHUTTLE',6,'Korsten','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(201,1,'ABC 123 EC','SHUTTLE',3,'Central','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(202,1,'ABC 123 EC','SHUTTLE',8,'Central','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(203,1,'ABC 123 EC','SHUTTLE',5,'Korsten','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(204,1,'ABC 123 EC','SHUTTLE',2,'Gomery Shuttle Stop','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,24),(205,1,'ABC 123 EC','SHUTTLE',7,'Gomery Shuttle Stop','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,24),(206,1,'ABC 123 EC','SHUTTLE',4,'SSSV Residences','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,23),(207,1,'ABC 123 EC','SHUTTLE',1,'Summerstrand','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,22),(208,1,'ABC 123 EC','SHUTTLE',6,'Summerstrand','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,22),(209,1,'ABC 123 EC','SHUTTLE',3,'Forest Hill','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,21),(210,1,'ABC 123 EC','SHUTTLE',8,'Forest Hill','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,21),(211,1,'ABC 123 EC','SHUTTLE',5,'Pier 14','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,20),(212,1,'ABC 123 EC','SHUTTLE',2,'Humewood','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,19),(213,1,'ABC 123 EC','SHUTTLE',7,'Humewood','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,19),(214,1,'ABC 123 EC','SHUTTLE',4,'Walmer Blvd','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,18),(215,1,'ABC 123 EC','SHUTTLE',1,'Walmer','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,17),(216,1,'ABC 123 EC','SHUTTLE',6,'Walmer','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,17),(217,1,'ABC 123 EC','SHUTTLE',3,'Central','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,16),(218,1,'ABC 123 EC','SHUTTLE',8,'Central','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,16),(219,1,'ABC 123 EC','SHUTTLE',5,'Rink Street','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,15),(220,1,'ABC 123 EC','SHUTTLE',2,'Feather Market Hall','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,14),(221,1,'ABC 123 EC','SHUTTLE',7,'Feather Market Hall','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,14),(222,1,'ABC 123 EC','SHUTTLE',4,'Russell Road','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,13),(223,1,'ABC 123 EC','SHUTTLE',1,'Richmond Hill','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,12),(224,1,'ABC 123 EC','SHUTTLE',6,'Richmond Hill','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,12),(225,1,'ABC 123 EC','SHUTTLE',3,'Varsity Park','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,11),(226,1,'ABC 123 EC','SHUTTLE',8,'Varsity Park','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,11),(227,1,'ABC 123 EC','SHUTTLE',5,'Sydenham','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,10),(228,1,'ABC 123 EC','SHUTTLE',2,'Korsten','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,9),(229,1,'ABC 123 EC','SHUTTLE',7,'Korsten','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,9),(230,1,'ABC 123 EC','SHUTTLE',4,'North Campus','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(231,1,'ABC 123 EC','SHUTTLE',1,'North Campus','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(232,1,'ABC 123 EC','SHUTTLE',6,'North Campus','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(233,2,'XYZ 789 EC','SHUTTLE',2,'Summerstrand','Summerstrand Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,7),(234,2,'XYZ 789 EC','SHUTTLE',7,'Summerstrand','Summerstrand Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,7),(235,2,'XYZ 789 EC','SHUTTLE',4,'Summerstrand','Summerstrand Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,3),(236,2,'XYZ 789 EC','SHUTTLE',1,'Gomery Shuttle Stop','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,40),(237,2,'XYZ 789 EC','SHUTTLE',6,'Gomery Shuttle Stop','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,40),(238,2,'XYZ 789 EC','SHUTTLE',3,'SSSV Residences','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,39),(239,2,'XYZ 789 EC','SHUTTLE',8,'SSSV Residences','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,39),(240,2,'XYZ 789 EC','SHUTTLE',5,'Summerstrand','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,38),(241,2,'XYZ 789 EC','SHUTTLE',2,'Forest Hill','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,37),(242,2,'XYZ 789 EC','SHUTTLE',7,'Forest Hill','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,37),(243,2,'XYZ 789 EC','SHUTTLE',4,'Pier 14','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,36),(244,2,'XYZ 789 EC','SHUTTLE',1,'Humewood','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,35),(245,2,'XYZ 789 EC','SHUTTLE',6,'Humewood','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,35),(246,2,'XYZ 789 EC','SHUTTLE',3,'Walmer Blvd','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,34),(247,2,'XYZ 789 EC','SHUTTLE',8,'Walmer Blvd','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,34),(248,2,'XYZ 789 EC','SHUTTLE',5,'Walmer','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,33),(249,2,'XYZ 789 EC','SHUTTLE',2,'Central','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,32),(250,2,'XYZ 789 EC','SHUTTLE',7,'Central','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,32),(251,2,'XYZ 789 EC','SHUTTLE',4,'Rink Street','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,31),(252,2,'XYZ 789 EC','SHUTTLE',1,'Feather Market Hall','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,30),(253,2,'XYZ 789 EC','SHUTTLE',6,'Feather Market Hall','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,30),(254,2,'XYZ 789 EC','SHUTTLE',3,'Russell Road','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,29),(255,2,'XYZ 789 EC','SHUTTLE',8,'Russell Road','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,29),(256,2,'XYZ 789 EC','SHUTTLE',5,'Richmond Hill','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,28),(257,2,'XYZ 789 EC','SHUTTLE',2,'Varsity Park','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,27),(258,2,'XYZ 789 EC','SHUTTLE',7,'Varsity Park','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,27),(259,2,'XYZ 789 EC','SHUTTLE',4,'Sydenham','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,26),(260,2,'XYZ 789 EC','SHUTTLE',1,'Korsten','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,25),(261,2,'XYZ 789 EC','SHUTTLE',6,'Korsten','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,25),(262,2,'XYZ 789 EC','SHUTTLE',3,'Central','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(263,2,'XYZ 789 EC','SHUTTLE',8,'Central','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(264,2,'XYZ 789 EC','SHUTTLE',5,'Korsten','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(265,2,'XYZ 789 EC','SHUTTLE',2,'Central','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(266,2,'XYZ 789 EC','SHUTTLE',7,'Central','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(267,2,'XYZ 789 EC','SHUTTLE',4,'Korsten','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(268,2,'XYZ 789 EC','SHUTTLE',1,'Gomery Shuttle Stop','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,24),(269,2,'XYZ 789 EC','SHUTTLE',6,'Gomery Shuttle Stop','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,24),(270,2,'XYZ 789 EC','SHUTTLE',3,'SSSV Residences','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,23),(271,2,'XYZ 789 EC','SHUTTLE',8,'SSSV Residences','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,23),(272,2,'XYZ 789 EC','SHUTTLE',5,'Summerstrand','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,22),(273,2,'XYZ 789 EC','SHUTTLE',2,'Forest Hill','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,21),(274,2,'XYZ 789 EC','SHUTTLE',7,'Forest Hill','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,21),(275,2,'XYZ 789 EC','SHUTTLE',4,'Pier 14','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,20),(276,2,'XYZ 789 EC','SHUTTLE',1,'Humewood','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,19),(277,2,'XYZ 789 EC','SHUTTLE',6,'Humewood','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,19),(278,2,'XYZ 789 EC','SHUTTLE',3,'Walmer Blvd','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,18),(279,2,'XYZ 789 EC','SHUTTLE',8,'Walmer Blvd','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,18),(280,2,'XYZ 789 EC','SHUTTLE',5,'Walmer','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,17),(281,2,'XYZ 789 EC','SHUTTLE',2,'Central','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,16),(282,2,'XYZ 789 EC','SHUTTLE',7,'Central','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,16),(283,2,'XYZ 789 EC','SHUTTLE',4,'Rink Street','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,15),(284,2,'XYZ 789 EC','SHUTTLE',1,'Feather Market Hall','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,14),(285,2,'XYZ 789 EC','SHUTTLE',6,'Feather Market Hall','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,14),(286,2,'XYZ 789 EC','SHUTTLE',3,'Russell Road','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,13),(287,2,'XYZ 789 EC','SHUTTLE',8,'Russell Road','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,13),(288,2,'XYZ 789 EC','SHUTTLE',5,'Richmond Hill','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,12),(289,2,'XYZ 789 EC','SHUTTLE',2,'Varsity Park','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,11),(290,2,'XYZ 789 EC','SHUTTLE',7,'Varsity Park','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,11),(291,2,'XYZ 789 EC','SHUTTLE',4,'Sydenham','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,10),(292,2,'XYZ 789 EC','SHUTTLE',1,'Korsten','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,9),(293,2,'XYZ 789 EC','SHUTTLE',6,'Korsten','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,9),(294,2,'XYZ 789 EC','SHUTTLE',3,'North Campus','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(295,2,'XYZ 789 EC','SHUTTLE',8,'North Campus','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(296,2,'XYZ 789 EC','SHUTTLE',5,'North Campus','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(297,8,'NMU005EC','SHUTTLE',1,'Summerstrand','Summerstrand Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,7),(298,8,'NMU005EC','SHUTTLE',6,'Summerstrand','Summerstrand Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,7),(299,8,'NMU005EC','SHUTTLE',3,'Summerstrand','Summerstrand Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,3),(300,8,'NMU005EC','SHUTTLE',8,'Summerstrand','Summerstrand Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,3),(301,8,'NMU005EC','SHUTTLE',5,'Gomery Shuttle Stop','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,40),(302,8,'NMU005EC','SHUTTLE',2,'SSSV Residences','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,39),(303,8,'NMU005EC','SHUTTLE',7,'SSSV Residences','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,39),(304,8,'NMU005EC','SHUTTLE',4,'Summerstrand','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,38),(305,8,'NMU005EC','SHUTTLE',1,'Forest Hill','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,37),(306,8,'NMU005EC','SHUTTLE',6,'Forest Hill','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,37),(307,8,'NMU005EC','SHUTTLE',3,'Pier 14','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,36),(308,8,'NMU005EC','SHUTTLE',8,'Pier 14','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,36),(309,8,'NMU005EC','SHUTTLE',5,'Humewood','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,35),(310,8,'NMU005EC','SHUTTLE',2,'Walmer Blvd','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,34),(311,8,'NMU005EC','SHUTTLE',7,'Walmer Blvd','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,34),(312,8,'NMU005EC','SHUTTLE',4,'Walmer','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,33),(313,8,'NMU005EC','SHUTTLE',1,'Central','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,32),(314,8,'NMU005EC','SHUTTLE',6,'Central','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,32),(315,8,'NMU005EC','SHUTTLE',3,'Rink Street','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,31),(316,8,'NMU005EC','SHUTTLE',8,'Rink Street','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,31),(317,8,'NMU005EC','SHUTTLE',5,'Feather Market Hall','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,30),(318,8,'NMU005EC','SHUTTLE',2,'Russell Road','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,29),(319,8,'NMU005EC','SHUTTLE',7,'Russell Road','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,29),(320,8,'NMU005EC','SHUTTLE',4,'Richmond Hill','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,28),(321,8,'NMU005EC','SHUTTLE',1,'Varsity Park','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,27),(322,8,'NMU005EC','SHUTTLE',6,'Varsity Park','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,27),(323,8,'NMU005EC','SHUTTLE',3,'Sydenham','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,26),(324,8,'NMU005EC','SHUTTLE',8,'Sydenham','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,26),(325,8,'NMU005EC','SHUTTLE',5,'Korsten','North Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,25),(326,8,'NMU005EC','SHUTTLE',2,'Central','North Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(327,8,'NMU005EC','SHUTTLE',7,'Central','North Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,8),(328,8,'NMU005EC','SHUTTLE',4,'Korsten','North Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,6),(329,8,'NMU005EC','SHUTTLE',1,'Central','North Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(330,8,'NMU005EC','SHUTTLE',6,'Central','North Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,4),(331,8,'NMU005EC','SHUTTLE',3,'Korsten','North Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(332,8,'NMU005EC','SHUTTLE',8,'Korsten','North Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,2),(333,8,'NMU005EC','SHUTTLE',5,'Gomery Shuttle Stop','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,24),(334,8,'NMU005EC','SHUTTLE',2,'SSSV Residences','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,23),(335,8,'NMU005EC','SHUTTLE',7,'SSSV Residences','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,23),(336,8,'NMU005EC','SHUTTLE',4,'Summerstrand','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,22),(337,8,'NMU005EC','SHUTTLE',1,'Forest Hill','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,21),(338,8,'NMU005EC','SHUTTLE',6,'Forest Hill','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,21),(339,8,'NMU005EC','SHUTTLE',3,'Pier 14','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,20),(340,8,'NMU005EC','SHUTTLE',8,'Pier 14','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,20),(341,8,'NMU005EC','SHUTTLE',5,'Humewood','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,19),(342,8,'NMU005EC','SHUTTLE',2,'Walmer Blvd','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,18),(343,8,'NMU005EC','SHUTTLE',7,'Walmer Blvd','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,18),(344,8,'NMU005EC','SHUTTLE',4,'Walmer','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,17),(345,8,'NMU005EC','SHUTTLE',1,'Central','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,16),(346,8,'NMU005EC','SHUTTLE',6,'Central','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,16),(347,8,'NMU005EC','SHUTTLE',3,'Rink Street','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,15),(348,8,'NMU005EC','SHUTTLE',8,'Rink Street','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,15),(349,8,'NMU005EC','SHUTTLE',5,'Feather Market Hall','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,14),(350,8,'NMU005EC','SHUTTLE',2,'Russell Road','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,13),(351,8,'NMU005EC','SHUTTLE',7,'Russell Road','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,13),(352,8,'NMU005EC','SHUTTLE',4,'Richmond Hill','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,12),(353,8,'NMU005EC','SHUTTLE',1,'Varsity Park','South Campus','2026-08-04 06:45:00','2026-08-04 07:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,11),(354,8,'NMU005EC','SHUTTLE',6,'Varsity Park','South Campus','2026-08-04 14:30:00','2026-08-04 15:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,11),(355,8,'NMU005EC','SHUTTLE',3,'Sydenham','South Campus','2026-08-04 08:45:00','2026-08-04 09:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,10),(356,8,'NMU005EC','SHUTTLE',8,'Sydenham','South Campus','2026-08-04 17:30:00','2026-08-04 18:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,10),(357,8,'NMU005EC','SHUTTLE',5,'Korsten','South Campus','2026-08-04 12:30:00','2026-08-04 13:15:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,9),(358,8,'NMU005EC','SHUTTLE',2,'North Campus','South Campus','2026-08-04 07:45:00','2026-08-04 08:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(359,8,'NMU005EC','SHUTTLE',7,'North Campus','South Campus','2026-08-04 16:00:00','2026-08-04 16:45:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,5),(360,8,'NMU005EC','SHUTTLE',4,'North Campus','South Campus','2026-08-04 09:45:00','2026-08-04 10:30:00',22,0.00,'SCHEDULED',NULL,NULL,NULL,NULL,NULL,NULL,0,0,NULL,1),(552,3,'CA123456','Carpool',NULL,'South Campus','Missionvale Campus','2026-08-08 07:30:00',NULL,1,35.00,'SCHEDULED',-34.00809,25.67319,-33.87253,25.55223,NULL,NULL,0,0,NULL,NULL),(553,6,'CA999888','Carpool',NULL,'South Campus','Missionvale Campus','2026-08-09 08:00:00',NULL,0,30.00,'SCHEDULED',-34.00809,25.67319,-33.87253,25.55223,NULL,NULL,0,0,NULL,NULL),(554,3,'CA123456','Carpool',NULL,'South Campus','Missionvale Campus','2026-07-20 07:15:00','2026-07-20 08:05:00',2,35.00,'COMPLETED',-34.00809,25.67319,-33.87253,25.55223,NULL,NULL,0,0,NULL,NULL),(555,3,'CA123456','Carpool',NULL,'South Campus','Missionvale Campus','2026-08-09 07:30:00',NULL,2,35.00,'SCHEDULED',-34.00809,25.67319,-33.87253,25.55223,NULL,NULL,0,0,NULL,NULL),(556,6,'CA999888','Carpool',NULL,'South Campus','Missionvale Campus','2026-08-09 08:00:00',NULL,0,30.00,'SCHEDULED',-34.00809,25.67319,-33.87253,25.55223,NULL,NULL,0,0,NULL,NULL),(557,3,'CA123456','Carpool',NULL,'South Campus','Missionvale Campus','2026-07-20 07:15:00','2026-07-20 08:05:00',2,35.00,'COMPLETED',-34.00809,25.67319,-33.87253,25.55223,NULL,NULL,0,0,NULL,NULL);
/*!40000 ALTER TABLE `trip` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trip_booking`
--

DROP TABLE IF EXISTS `trip_booking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trip_booking` (
  `booking_id` bigint NOT NULL AUTO_INCREMENT,
  `trip_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `booking_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `booking_status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`booking_id`),
  KEY `fk_booking_trip` (`trip_id`),
  KEY `fk_booking_student` (`student_id`),
  CONSTRAINT `fk_booking_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_booking_trip` FOREIGN KEY (`trip_id`) REFERENCES `trip` (`trip_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trip_booking`
--

LOCK TABLES `trip_booking` WRITE;
/*!40000 ALTER TABLE `trip_booking` DISABLE KEYS */;
INSERT INTO `trip_booking` VALUES (1,28,3,'2026-08-01 19:41:20','CANCELLED'),(2,50,3,'2026-08-04 15:43:19','CONFIRMED'),(3,552,1,'2026-08-05 19:07:06','CONFIRMED'),(4,555,1,'2026-08-31 13:19:41','CONFIRMED');
/*!40000 ALTER TABLE `trip_booking` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trip_leg_route`
--

DROP TABLE IF EXISTS `trip_leg_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trip_leg_route` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trip_id` bigint NOT NULL,
  `from_stop_order` int NOT NULL,
  `to_stop_order` int NOT NULL,
  `route_geometry` json NOT NULL,
  `distance_meters` double DEFAULT NULL,
  `duration_seconds` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_leg_trip_id` (`trip_id`),
  CONSTRAINT `fk_leg_trip` FOREIGN KEY (`trip_id`) REFERENCES `trip` (`trip_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trip_leg_route`
--

LOCK TABLES `trip_leg_route` WRITE;
/*!40000 ALTER TABLE `trip_leg_route` DISABLE KEYS */;
/*!40000 ALTER TABLE `trip_leg_route` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trip_location_history`
--

DROP TABLE IF EXISTS `trip_location_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trip_location_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trip_id` bigint NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `recorded_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_history_trip_id` (`trip_id`),
  CONSTRAINT `fk_history_trip` FOREIGN KEY (`trip_id`) REFERENCES `trip` (`trip_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trip_location_history`
--

LOCK TABLES `trip_location_history` WRITE;
/*!40000 ALTER TABLE `trip_location_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `trip_location_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trip_review`
--

DROP TABLE IF EXISTS `trip_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trip_review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` bigint NOT NULL,
  `rating` int DEFAULT NULL,
  `review` text,
  `review_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  UNIQUE KEY `booking_id` (`booking_id`),
  CONSTRAINT `fk_review_booking` FOREIGN KEY (`booking_id`) REFERENCES `trip_booking` (`booking_id`) ON DELETE CASCADE,
  CONSTRAINT `trip_review_chk_1` CHECK ((`rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trip_review`
--

LOCK TABLES `trip_review` WRITE;
/*!40000 ALTER TABLE `trip_review` DISABLE KEYS */;
/*!40000 ALTER TABLE `trip_review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trip_stop`
--

DROP TABLE IF EXISTS `trip_stop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trip_stop` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trip_id` bigint NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `stop_name` varchar(255) DEFAULT NULL,
  `stop_order` int NOT NULL,
  `status` enum('PENDING','ARRIVED') NOT NULL DEFAULT 'PENDING',
  `student_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_trip_stop_trip_id` (`trip_id`),
  KEY `FKlx3nwtyilnv0m6newdeqfa76u` (`student_id`),
  CONSTRAINT `fk_trip_stop_trip` FOREIGN KEY (`trip_id`) REFERENCES `trip` (`trip_id`) ON DELETE CASCADE,
  CONSTRAINT `FKlx3nwtyilnv0m6newdeqfa76u` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trip_stop`
--

LOCK TABLES `trip_stop` WRITE;
/*!40000 ALTER TABLE `trip_stop` DISABLE KEYS */;
INSERT INTO `trip_stop` VALUES (1,1,-33.9912,25.6698,'North Campus Gate 1',1,'ARRIVED',NULL),(2,1,-33.995,25.672,'Library Stop',2,'ARRIVED',NULL),(3,1,-33.9984,25.675,'South Campus Terminal',3,'ARRIVED',NULL),(4,2,-34.0021,25.6601,'Student House A',1,'PENDING',1),(5,2,-33.91,25.55,'Missionvale Campus',2,'PENDING',NULL),(6,3,-33.9457,25.5661,'Newton Park, Cape Road Spar',1,'PENDING',1),(7,3,-33.9914,25.6569,'2nd Avenue Campus',2,'PENDING',1),(8,6,-33.9758,25.5858,'Walmer, 6th Avenue',1,'PENDING',1),(9,24,-33.9914,25.6569,'Summerstrand, Gqeberha, EC, South Africa',1,'PENDING',1),(10,24,-33.9912,25.6698,'North Campus',2,'PENDING',1),(18,6,-33.9984,25.675,'South Campus',2,'PENDING',1),(19,25,-34.0021,25.6601,'Summerstrand',1,'PENDING',1),(20,25,-34.0021,25.6601,'Summerstrand',2,'PENDING',1),(21,25,-34.0021,25.6601,'Summerstrand',3,'PENDING',1),(22,552,-34.00809,25.67319,'South Campus',1,'PENDING',1),(23,552,-34.00809,25.67319,'South Campus',2,'PENDING',1),(24,555,-34.00809,25.67319,'South Campus',1,'PENDING',1),(25,555,-34.00809,25.67319,'South Campus',2,'PENDING',1),(26,555,-33.9755999,25.6405641,'15 Killarney Road, Humewood, Gqeberha, South Africa',3,'PENDING',1);
/*!40000 ALTER TABLE `trip_stop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `UserID` bigint NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL,
  PRIMARY KEY (`UserID`),
  UNIQUE KEY `unique_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Admin','User','admin@getyourride.com','1234','ADMIN'),(2,'Mandy','Mhlongo','coord@getyourride.com','1234','COORDINATOR');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicle`
--

DROP TABLE IF EXISTS `vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle` (
  `vehicle_id` bigint NOT NULL AUTO_INCREMENT,
  `driver_id` bigint NOT NULL,
  `registration_number` varchar(20) NOT NULL,
  `model` varchar(255) DEFAULT NULL,
  `vehicle_year` int DEFAULT NULL,
  `colour` varchar(255) DEFAULT NULL,
  `capacity` int NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'Active',
  PRIMARY KEY (`vehicle_id`),
  UNIQUE KEY `registration_number` (`registration_number`),
  KEY `fk_vehicle_driver` (`driver_id`),
  CONSTRAINT `fk_vehicle_driver` FOREIGN KEY (`driver_id`) REFERENCES `driver` (`driver_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle`
--

LOCK TABLES `vehicle` WRITE;
/*!40000 ALTER TABLE `vehicle` DISABLE KEYS */;
INSERT INTO `vehicle` VALUES (1,1,'NMU001EC','Toyota Quantum',2021,'White',15,'Active'),(2,2,'NMU002EC','Toyota Quantum',2022,'White',15,'Active'),(3,3,'CA123456','VW Polo Vivo',2019,'Silver',4,'Active'),(4,4,'CA654321','Toyota Corolla',2020,'Blue',4,'Active'),(5,5,'CA111222','Hyundai i20',2018,'Red',4,'Active'),(6,6,'CA999888','Ford Fiesta',2017,'Black',4,'Active'),(7,7,'CB123456','Toyota Corolla',2022,'White',4,'Active'),(8,1,'ABC 123 EC','Toyota Quantum',2020,'White',15,'Active'),(9,2,'XYZ 789 EC','VW Polo',2018,'Silver',4,'Active'),(10,8,'NMU005EC','Toyota Quantum',2023,'White',22,'Active');
/*!40000 ALTER TABLE `vehicle` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-31 17:48:51
