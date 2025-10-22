# Deployment Guide for cheap-rest

## Quick Start with Docker

### PostgreSQL (Recommended)

```bash
cd cheap-rest
docker-compose up -d
```

API available at: http://localhost:8080

### SQLite

```bash
cd cheap-rest
docker-compose -f docker-compose-sqlite.yml up -d
```

### MariaDB

```bash
cd cheap-rest
export DB_PASSWORD=yourpassword
docker-compose -f docker-compose-mariadb.yml up -d
```

## Running from JAR

### Build

```bash
./gradlew :cheap-rest:bootJar
```

### Run

```bash
# PostgreSQL
export DB_PASSWORD=yourpassword
java -jar cheap-rest/build/libs/cheap-rest-0.1.jar --spring.profiles.active=postgres

# SQLite
java -jar cheap-rest/build/libs/cheap-rest-0.1.jar --spring.profiles.active=sqlite

# MariaDB
export DB_PASSWORD=yourpassword
java -jar cheap-rest/build/libs/cheap-rest-0.1.jar --spring.profiles.active=mariadb
```

## Database Setup

### PostgreSQL

```sql
CREATE DATABASE cheap;
CREATE USER cheap_user WITH ENCRYPTED PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE cheap TO cheap_user;
```

### MariaDB

```sql
CREATE DATABASE cheap;
CREATE USER 'cheap_user'@'localhost' IDENTIFIED BY 'yourpassword';
GRANT ALL PRIVILEGES ON cheap.* TO 'cheap_user'@'localhost';
FLUSH PRIVILEGES;
```

### SQLite

No setup required. Database file created automatically.

## Configuration

### Environment Variables

- `SPRING_PROFILES_ACTIVE` - Database profile (postgres/sqlite/mariadb)
- `DB_PASSWORD` - Database password
- `CHEAP_DB_PATH` - SQLite database file path (default: ./cheap.db)

### Application Properties

Edit `application-{profile}.yml` files or set via environment:

```yaml
cheap:
  pagination:
    default-page-size: 20
    max-page-size: 100
  aspect-upsert:
    max-batch-size: 1000
```

## Production Deployment

### System Requirements

- 2+ CPU cores
- 4+ GB RAM
- 10+ GB disk space

### Recommended JVM Options

```bash
JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"
java $JAVA_OPTS -jar cheap-rest-0.1.jar
```

### Using systemd (Linux)

Create `/etc/systemd/system/cheap-rest.service`:

```ini
[Unit]
Description=Cheap REST API
After=postgresql.service

[Service]
Type=simple
User=cheap
Environment="SPRING_PROFILES_ACTIVE=postgres"
Environment="DB_PASSWORD=yourpassword"
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/cheap-rest/cheap-rest-0.1.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl enable cheap-rest
sudo systemctl start cheap-rest
```

## Monitoring

Health check endpoint:

```
GET http://localhost:8080/actuator/health
```

View logs:

```bash
# Docker
docker logs cheap-rest

# systemd
sudo journalctl -u cheap-rest -f
```

## Backup

### PostgreSQL

```bash
pg_dump -U cheap_user -d cheap > cheap_backup.sql
```

### SQLite

```bash
cp /data/cheap.db cheap_backup.db
```

### MariaDB

```bash
mysqldump -u cheap_user -p cheap > cheap_backup.sql
```

## Troubleshooting

### Check database connectivity

```bash
# PostgreSQL
psql -h localhost -U cheap_user -d cheap

# MariaDB
mysql -h localhost -u cheap_user -p cheap
```

### View application logs

```bash
# Docker
docker logs cheap-rest -f

# Local
tail -f logs/spring.log
```

### Port already in use

```bash
java -jar cheap-rest-0.1.jar --server.port=8081
```

## Security

1. Use strong database passwords
2. Enable HTTPS in production (use reverse proxy)
3. Restrict database network access
4. Keep dependencies updated
5. Implement rate limiting at proxy level

For detailed configuration and advanced deployment scenarios, see README.md.
