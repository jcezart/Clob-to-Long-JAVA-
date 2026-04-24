<div align="center">

# 🧹 ClobToLong

**A Java utility deployed inside Oracle Database to extract, clean, and persist CLOB JSON data into relational columns**

![Java](https://img.shields.io/badge/Java-Oracle_JVM-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Oracle DB](https://img.shields.io/badge/Oracle_Database-Java_Stored_Procedure-F80000?style=for-the-badge&logo=oracle&logoColor=white)
![PL/SQL](https://img.shields.io/badge/PL%2FSQL-Callable-F80000?style=for-the-badge&logo=oracle&logoColor=white)
![Regex](https://img.shields.io/badge/Text_Processing-Regex-6A0DAD?style=for-the-badge)
![JDBC](https://img.shields.io/badge/JDBC-OracleDriver-0052CC?style=for-the-badge)

</div>

---

## 📖 About

**ClobToLong** is a Java class compiled and stored directly inside an Oracle Database as a **Java Stored Procedure**. It was built to solve a real-world data processing problem: CLOB columns containing large, noisy JSON payloads from ticketing/comment systems needed to be parsed, sanitized, and stored as clean plain text in `LONG` or `VARCHAR2` database columns.

The class is deployed using Oracle's `CREATE OR REPLACE AND COMPILE JAVA SOURCE` statement, making it callable from PL/SQL without any external application layer.

---

## ✨ Features

- 📦 **Java inside Oracle** — deployed as a native Java Stored Procedure, callable directly from PL/SQL
- 🔍 **JSON Parsing without libraries** — manually traverses `"body"` fields in a `comments` JSON array using index-based string search, with no external dependency
- 🧹 **Multi-layer text cleaning** — strips HTML tags, Markdown syntax (links, images, headers), raw URLs, domain patterns, confidentiality disclaimers, and escape sequences
- 🔄 **CLOB to String conversion** — reads Oracle `CLOB` data using `getSubString` for efficient in-memory processing
- 🗄️ **Direct DB update** — persists cleaned text to target tables using `PreparedStatement` with parameterized queries
- 🔐 **Safe resource management** — uses `try-catch-finally` blocks to guarantee `Connection` and `PreparedStatement` are always closed
- 🌐 **Bilingual disclaimer removal** — strips confidentiality footers in both Portuguese and English

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java (Oracle JVM — JDK 8+) |
| Deployment | `CREATE OR REPLACE AND COMPILE JAVA SOURCE` |
| JDBC Driver | `oracle.jdbc.driver.OracleDriver` |
| DB Connection | `OracleDriver().defaultConnection()` (in-process) |
| Text Processing | `String.replaceAll()` with compiled Regex patterns |
| JSON Parsing | Manual index-based traversal (`indexOf` / `substring`) |
| Data Type | Oracle `CLOB` → Java `String` → Oracle `LONG`/`VARCHAR2` |

---

## 🔍 Key Implementation Details

### Deployment as Java Stored Procedure

The class is not compiled externally — it lives inside the Oracle Database itself, loaded via a DDL statement. This makes it invokable from any PL/SQL block with no middleware.

```sql
CREATE OR REPLACE AND COMPILE JAVA SOURCE NAMED "UtilClobToLong" AS
  -- Java source code here
/
```

After compilation, a PL/SQL wrapper exposes it:

```sql
CREATE OR REPLACE PROCEDURE UPDATE_TABLE(p_clob IN CLOB, p_key IN NUMBER) AS
LANGUAGE JAVA NAME 'ClobtoLong.UpdateTable(java.sql.Clob, int)';
```

### CLOB to String Conversion

Oracle `CLOB` objects are converted to a Java `String` using `getSubString`, reading the full content from position 1 up to the CLOB's length.

```java
String jsonString = entrada.getSubString(1, (int) entrada.length());
```

### Manual JSON Parsing — No External Libraries

Since the Oracle JVM environment doesn't allow arbitrary third-party jars, the JSON parsing is done manually using `indexOf` and `substring` to locate and extract every `"body"` field inside the `comments` array.

```java
String bodyMarker = "\"body\":\"";
int startIndex = 0;

while (true) {
    startIndex = jsonString.indexOf(bodyMarker, startIndex);
    if (startIndex == -1) break;

    startIndex += bodyMarker.length();
    int endIndex = jsonString.indexOf("\",", startIndex);
    if (endIndex == -1) endIndex = jsonString.indexOf("\"}", startIndex);
    if (endIndex == -1) break;

    String bodyContent = jsonString.substring(startIndex, endIndex);
    // decode + clean...
    startIndex = endIndex;
}
```

### Multi-layer Text Cleaning with Regex

The `limparTexto` method applies a sequential chain of `replaceAll` calls to sanitize the extracted text:

```java
// 1. Remove HTML tags
textoOriginal = textoOriginal.replaceAll("<[^>]+>", "");

// 2. Remove Markdown images and links
textoOriginal = textoOriginal.replaceAll("!\\[.*?\\]\\(.*?\\)", "");
textoOriginal = textoOriginal.replaceAll("\\[.*?\\]\\(.*?\\)", "");

// 3. Remove URLs and domain patterns
textoOriginal = textoOriginal.replaceAll("[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+", "");

// 4. Remove bilingual confidentiality disclaimers (PT + EN)
textoOriginal = textoOriginal.replaceAll("Esta mensagem.*?Agradecemos sua cooperação\\.", "");
textoOriginal = textoOriginal.replaceAll("This message.*?We appreciate your cooperation\\.", "");

// 5. Remove Markdown headers, escape sequences, *, #, --
textoOriginal = textoOriginal.replaceAll("(?m)^#+\\s*", "");
textoOriginal = textoOriginal.replaceAll("\\\\n\\\\n", " ");
textoOriginal = textoOriginal.replaceAll("[*]+", "");

// 6. Normalize whitespace and add spacing after punctuation
textoOriginal = textoOriginal.replaceAll("[\\t\\s]+", " ");
textoOriginal = textoOriginal.replaceAll("([,.?!])([^\\s])", "$1 $2");
```

### Database Update with PreparedStatement

Cleaned text is persisted using a parameterized `UPDATE` statement, preventing SQL injection and correctly handling special characters.

```java
String sql = "UPDATE TABLE TB SET TB.COLUMN = ? WHERE TB.PARAMETER = ?";
pstmt = con.prepareStatement(sql);
pstmt.setString(1, texto);
pstmt.setInt(2, chave);
pstmt.executeUpdate();
```

### In-Process Connection

Instead of a traditional JDBC connection string, the class uses `OracleDriver().defaultConnection()` — Oracle's special in-process connection that reuses the current session's context with zero overhead.

```java
con = new OracleDriver().defaultConnection();
```

---

## 🔄 Processing Pipeline

```
Input: Oracle CLOB (JSON with "comments" array)
  │
  ├── 1. getSubString() → full JSON as Java String
  ├── 2. Loop: indexOf("\"body\":\"") → extract each body value
  ├── 3. Unescape: \n \r \t \" \\
  ├── 4. limparTexto():
  │       ├── Strip HTML tags
  │       ├── Strip Markdown links & images
  │       ├── Strip URLs & domains
  │       ├── Strip confidentiality footers (PT + EN)
  │       ├── Strip Markdown headers & symbols
  │       ├── Normalize whitespace
  │       └── Fix punctuation spacing
  ├── 5. Concatenate all cleaned bodies with spaces
  └── 6. PreparedStatement UPDATE → target table column
```

---

## 📥 Input / Output Example

**Input CLOB:**
```json
{
  "comments": [
    {"body": "Hello **world**! Visit [docs](http://example.com)."},
    {"body": "Follow-up: issue is #resolved.\nThis message, including its attachments, may contain confidential information... We appreciate your cooperation."}
  ]
}
```

**Output (stored in DB column):**
```
Hello world! Follow-up: issue is resolved.
```

---

## ⚙️ Setup

1. **Deploy the Java source** into your Oracle Database using a SQL client (SQL\*Plus, SQL Developer, etc.):

```sql
@clob_to_long.java
```

2. **Create the PL/SQL wrappers** to expose the static methods:

```sql
CREATE OR REPLACE PROCEDURE UPDATE_TABLE_1(p_clob IN CLOB, p_key IN NUMBER)
AS LANGUAGE JAVA NAME 'ClobtoLong.UpdateTable(java.sql.Clob, int)';
/

CREATE OR REPLACE PROCEDURE UPDATE_TABLE_2(p_clob IN CLOB, p_key IN NUMBER)
AS LANGUAGE JAVA NAME 'ClobtoLong.UpdateTable2(java.sql.Clob, int)';
/
```

3. **Call from PL/SQL:**

```sql
DECLARE
    v_clob CLOB;
BEGIN
    SELECT JSON_DATA INTO v_clob FROM YOUR_TABLE WHERE ID = 12345;
    UPDATE_TABLE_1(v_clob, 12345);
END;
/
```

---

## 📚 Concepts Practiced

- **Java Stored Procedures** compiled and deployed inside Oracle Database
- **CLOB handling** in Java via `java.sql.Clob` and `getSubString`
- **Manual JSON parsing** without external libraries (Oracle JVM constraints)
- **Regex-based text processing** with multi-layer `replaceAll` chains
- **Oracle JDBC in-process connection** via `OracleDriver().defaultConnection()`
- **Parameterized SQL** with `PreparedStatement` for safe DB updates
- **Resource management** with `try-catch-finally` for connection cleanup
- **Escape sequence decoding** (`\n`, `\r`, `\t`, `\"`, `\\`)
- **Bilingual text pattern removal** (PT-BR + EN)

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

<div align="center">
  Made with ❤️ and Java · <a href="https://github.com/jcezart">@jcezart</a>
</div>
