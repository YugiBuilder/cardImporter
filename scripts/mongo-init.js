// MongoDB initialization script for Docker
// This script creates users and sets up authentication

// Switch to the database defined by MONGO_INITDB_DATABASE (yugioh_db)
db = db.getSiblingDB('yugioh_db');

// Create application user with readWrite privileges
db.createUser({
    user: 'your_username',
    pwd: 'your_password',
    roles: [
        {
            role: 'readWrite',
            db: 'yugioh_db'
        }
    ]
});

console.log('User created successfully for yugioh_db database');

// Optional: Create additional collections or perform other initialization
db.createCollection('cards');
db.createCollection('card_images');
db.createCollection('card_sets');

console.log('Collections created successfully');