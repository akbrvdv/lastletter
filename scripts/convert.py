import sqlite3
import re
import os

sql_path = r'd:\laragon\www\lastletter\dictionary_SQLite.sql'
assets_dir = r'd:\laragon\www\lastletter\app\src\main\assets\database'

os.makedirs(assets_dir, exist_ok=True)
db_path = os.path.join(assets_dir, 'kamus.db')

if os.path.exists(db_path):
    os.remove(db_path)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Create FTS4 table. Room's Fts4 requires no constraints usually, but simple table is enough.
cursor.execute("CREATE VIRTUAL TABLE words USING fts4(word)")

words_to_insert = set()

# Parse the SQL file
with open(sql_path, 'r', encoding='utf-8') as f:
    for line in f:
        match = re.search(r"^\s*\('([^']+)'", line)
        if match:
            word = match.group(1).strip().lower()
            if word:
                words_to_insert.add(word)

# Insert into the database
cursor.executemany("INSERT INTO words (word) VALUES (?)", [(w,) for w in words_to_insert])

conn.commit()
conn.close()
print(f"Inserted {len(words_to_insert)} unique words into {db_path}")
