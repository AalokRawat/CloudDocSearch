# CloudDocSearch

Application Overview:

This application serves as a file management and search system, utilizing two core services:

Google Cloud: This service is employed for storing files securely in the cloud. The application interfaces with Google Cloud using OAuth 2.0 for authentication. All configuration information required for communication with Google Cloud is consolidated in the credentials.json file.
Elasticsearch: Elasticsearch is the indexing and search engine used to efficiently organize and search through the stored files. To communicate with Elasticsearch, the application relies on a dedicated endpoint and API keys. These Elasticsearch configuration details are specified in the application.properties file. Please be aware that the Elasticsearch setup i provided is a trial version that will expire in ~10 days. If you intend to continue using it for testing beyond this period, you must update the elastic.* configurations in the application.properties file accordingly.
Application Startup Process:

Upon launching the application, a specific event known as "CloudDocSearchStartupEvent" is triggered. This event carries out the following tasks:

It scans the Cloud Storage to identify files with specific extensions, including .txt, .pdf, .csv, and .docx.
If any files are found to be missing or have been updated, the application takes appropriate actions to create or update them within the storage.
Searching for Files:

To retrieve files that contain specific text, you can utilize a RESTful API endpoint. Here's how it works:

HTTP Method: GET
URL: http://localhost:8080/search?q=<search_term>
By making a GET request to this URL and specifying the desired <search_term>, the application will respond by providing a list of file paths in which the given search term is present.
