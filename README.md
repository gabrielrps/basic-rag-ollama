# Basic RAG with Ollama & Spring AI

This project is a basic implementation of a **RAG (Retrieval-Augmented Generation)** system running entirely on local infrastructure. It allows users to upload documents and perform context-aware queries, ensuring the AI consults only specific files through metadata filtering.

## Technologies & Infrastructure

The project utilizes a modern stack designed for privacy and local processing:

*   **Java 21 & Spring Boot 3**: The core application framework.
*   **Spring AI**: A framework for seamless integration with LLMs and Vector Stores.
*   **Ollama (Llama 3)**: An inference engine to run the language model and embeddings locally.
*   **PostgreSQL + PGVector**: A relational database with vector similarity search capabilities.
*   **Liquibase**: For database schema versioning and management.
*   **Docker Compose**: Orchestration for the database and AI environment.

---

### 1. Ingestion Service (`IngestionService`)
This service prepares documents for the AI's "memory":
*   **Hash-Based Idempotency**: Before insertion, the system generates an MD5 hash of the file bytes. If the file already exists, old vectors are automatically deleted to prevent duplicate answers.
*   **`ByteArrayResource` Management**: Files are read into memory once, allowing for simultaneous hash calculation and text extraction without "stream closed" errors.
*   **Data Sanitization**: Automatically removes null characters (`0x00`) and control sequences that often cause persistence errors in PostgreSQL.
*   **Strategic Chunking**: Documents are split into 200-token blocks with a 50-token overlap to preserve context between segments.

### 2. Chat Service with Metadata Filtering (`ChatService`)
Unlike basic RAG systems, this engine supports targeted search:
*   **Pre-filtering**: Uses `FilterExpressionBuilder` to create a metadata filter based on the `file_hash`. This ensures the semantic search occurs **only** within the selected document.
*   **Intelligent Advisor**: The `QuestionAnswerAdvisor` intercepts the user's query, retrieves relevant context from the Vector Store, and instructs the model to provide accurate, document-based responses in **American English**.

---

## Setup and Execution

### Prerequisites
*   Docker and Docker Compose installed.
*   JDK 21 installed.

### Step-by-Step Instructions

1.  **Start Infrastructure**:
    ```bash
    cd docker
    docker-compose up -d
    ```

2.  **Pull the Model (Ollama)**:
    ```bash
    docker exec -it ollama ollama pull llama3
    ```

3.  **Run the Application**:
    ```bash
    ./mvnw spring-boot:run
    ```

---

## API Endpoints

### Document Ingestion
*   **URL**: `POST /ingestion/upload`
*   **Body**: `multipart/form-data` (PDF or TXT file)
*   **Action**: Saves the file, calculates the hash, and populates the Vector Store.

### Contextual Chat
*   **URL**: `POST /chat/ask`
*   **Payload**:
    ```json
    {
      "question": "What are the main points of this document?",
      "fileName": "book.pdf"
    }
    ```
*   **Response**:
    ```json
    {
    "response": "I'd be happy to help!The main points of this document are:\n\n1. A set of exercises on plane Euclidean geometry, covering topics such as triangles, polygons, circles, and angles.\n2. The exercises are grouped into four categories:\n\t* Area (problems 1-4): determining the area of various shapes, including triangles, polygons, and circles.\n\t* Segment measurement (problem 1): showing that there is a unique point C between two points A and B such that m(AC) = a for any real positive value a.\n\t* Circle properties (problem 2): approximating the length of a circle.\n\t* Angle properties (problems 1-3): exploring the properties of bisections, perpendicularity, and similarity.\n\nThese exercises seem to be focused on applying geometric concepts to solve problems."
    }
    ```    

---

## Privacy and Performance
*   **Isolation**: Every document is treated as a unique context via its Hash.
*   **Local-First**: No data leaves your machine. Processing is 100% private.
*   **Stability**: Custom exception handling for ingestion failures or file reading errors.