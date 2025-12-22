# HL7 Sync Middleware

## Overview
**HL7 Sync** is a Spring Boot middleware application designed to interface between OpenMRS and the HL7 Web Client. Its primary function is to extract patient demographic data from OpenMRS, convert it into encrypted HL7 (ADT_A24) messages, and manage the asynchronous file generation process.

## Key Features
* **Asynchronous Processing:** Handles file generation via a job queue to prevent timeouts on heavy data loads.
* **Dual-Database Connection:** Connects to its own primary database (for job management) and a secondary OpenMRS database (read-only for demographics).
* **Security:** Generates and encrypts HL7 files using a Java KeyStore.
* **Self-Healing:** Automatically detects and cleans up stale/stuck jobs upon application startup.
* **API-Driven:** Provides REST endpoints for generating files, checking status, and downloading results.

## Prerequisites
* **Java:** JDK 17
* **Maven:** 3.8+
* **Database:** MySQL 8.0+
* **OpenMRS:** An active instance of OpenMRS (RefApp)

## Configuration

The application relies on Environment Variables for configuration. You must set these in your OS or your IDE (Run Configuration) before starting the app. The following are example values

### Database Credentials
| Variable | Description | Example |
| :--- | :--- | :--- |
| `DB_URL` | JDBC URL for the HL7Sync internal DB | `jdbc:mysql://localhost:3306/hl7sync` |
| `DB_USERNAME` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | `root` |

### OpenMRS Connection
| Variable | Description | Example |
| :--- | :--- | :--- |
| `OPENMRS_DB` | JDBC URL for the OpenMRS DB | `jdbc:mysql://localhost:3306/openmrs` |
| `OPENMRS_URL` | URL of the OpenMRS Web API | `http://localhost:8080/openmrs/` |
| `OPENMRS_USERNAME` | OpenMRS API User | `admin` |
| `OPENMRS_PASSWORD` | OpenMRS API Password | `admin123` |

### Encryption & storage
| Variable | Description | Example |
| :--- | :--- | :--- |
| `app.keyStore` | Path to the .jceks KeyStore file | `/app/config/your_.jcekcs_file` |
| `app.keyStore.password` | Password for the KeyStore | `your_keystore_password` |
| `app.hl7.folder` | Output folder for generated files | `/opt/hl7/` |

## Installation & Running

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd hl7sync
    ```

2.  **Build with Maven:**
    ```bash
    mvn clean install
    ```

3.  **Run the Application:**
    ```bash
    cd api
    mvn spring-boot:run
    ```

## API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/demographics/generate` | Accepts a JSON payload with location details to queue a new generation job. |
| `GET` | `/api/demographics/status/{jobId}` | Checks the status (`QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`) of a specific job. |
| `GET` | `/api/demographics/download/{jobId}` | Downloads the encrypted ZIP package containing the HL7 file and metadata. |

## Architecture Note
This application uses **Flyway** for database migrations. On the first run, ensure your `hl7sync` database schema exists (even if empty), and Flyway will automatically create the necessary tables (`jobs`, etc.).

If you encounter `Migration checksum mismatch` errors during development, it usually means a migration file was modified after being applied. In a local dev environment, recreating the database (`DROP` and `CREATE`) is the standard fix.
