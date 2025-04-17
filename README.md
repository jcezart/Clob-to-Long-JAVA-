# 🚀 UtilClobToLong: Clean & Process CLOB Data 🧹
## Overview 🗃️
The UtilClobToLong Java utility is designed to process and clean CLOB (Character Large Object) data containing JSON-formatted text, specifically extracting and sanitizing content from "body" fields within a JSON array of comments. It is built for use in Oracle database environments and provides methods to update specific tables with the cleaned text.
This utility is particularly useful for applications that need to handle large text data, remove unwanted formatting (e.g., HTML, Markdown, URLs, and confidential disclaimers), and store the processed text in database columns.
## Features 📋

CLOB to String Conversion: Converts Oracle CLOB data to a Java String for processing.
Text Cleaning: Removes HTML tags, Markdown syntax (links, images, headers), URLs, confidentiality disclaimers, and excessive whitespace or special characters.
JSON Parsing: Extracts and processes all "body" fields from a JSON array of comments.
Database Updates: Provides methods to update specific columns in Oracle database tables (TABLE1 and TABLE2) with the cleaned text.
Error Handling: Includes robust exception handling for SQL and JSON processing errors.
Connection Management: Properly manages database connections and resources using try-catch-finally blocks.

## Components 🧩
The utility consists of a single Java class, ClobtoLong, with the following key methods:

limparTexto(String textoOriginal): Cleans the input text by removing HTML, Markdown, URLs, confidentiality notices, and excessive whitespace, while normalizing punctuation and formatting.
extractAndCleanAllBodies(Clob entrada): Extracts all "body" fields from the JSON in the CLOB, decodes escape characters, and applies the limparTexto cleaning process.
UpdateTable(Clob entrada, int chave): Updates the COLUMN column in the TABLE table for a given sequence number (PARAMETER).
UpdateTable2(Clob entrada, int chave): Updates the COLUMN2 column in the TABLE2 table for a given sequence number (PARAMETER2).

## Dependencies 🔗

Java: JDK 8 or higher.
Oracle JDBC Driver: Requires the Oracle JDBC driver (oracle.jdbc.driver.OracleDriver) for database connectivity.
Database: Oracle Database with access to the TABLE and TABLE2 tables.

## Usage 🛠️

Compile the Java Source:Deploy the UtilClobToLong Java source to your Oracle database using the provided create or replace and compile java source statement.

Invoke Methods:Call the static methods UpdateTable1 or UpdateTable2from your PL/SQL code, passing a CLOB containing JSON data and an integer key (PARAMETER).
Example PL/SQL invocation:
BEGIN
    UtilClobToLong.UpdateTable1(my_clob, 12345);
END;


Input Format:The CLOB should contain JSON with a "comments" array, where each comment has a "body" field. Example:
{
    "comments": [
        {"body": "This is a <b>test</b> with [link](http://example.com)"},
        {"body": "Another comment with #header"}
    ]
}


Output:The cleaned text from all "body" fields is concatenated with spaces and stored in the specified database column.


## Installation 💾

Clone this repository:git clone https://github.com/your-username/your-repo.git


Compile and deploy the Java source to your Oracle database using a SQL client (e.g., SQL*Plus or SQL Developer).
Ensure the Oracle JDBC driver is available in your database environment.

## Example 📝
Suppose you have a CLOB with the following JSON:
{
    "comments": [
        {"body": "Hello <b>world</b>! Visit [example](http://example.com)."},
        {"body": "This is a #test with confidential info.\nThis message, including its attachments, may contain confidential information..."}
    ]
}

Calling UpdatePlsHistLong(my_clob, 12345) will:

Extract and clean the "body" fields.
Produce a cleaned string: Hello world! This is a test with confidential info.
Update the COLUMN column in the TABLE table for PARAMETER = 12345.


