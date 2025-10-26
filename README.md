

# Kafka Spring Cloud Stream (PageEvent Analytics)

## 1. Présentation du projet

Ce projet est un exemple de **Spring Cloud Stream** avec Kafka Streams pour simuler et analyser des événements de pages web (`PageEvent`).

* On produit des événements aléatoires avec `pageEventSupplier`.
* On consomme les événements avec `pageEventConsumer`.
* On effectue des **calculs analytiques en temps réel** sur les événements via `KStreamFunction`.
* On expose un endpoint `/analytics` pour visualiser les données sur une **page web en temps réel** avec Smoothie Charts.

---

## 2. Structure des fichiers

```
src/main/java/ma/elamrani/kafkaspringcloudsteam/controllers/PageEventController.java
src/main/java/ma/elamrani/kafkaspringcloudsteam/events/PageEvent.java
src/main/java/ma/elamrani/kafkaspringcloudsteam/handlers/PageEventHandler.java
src/main/resources/application.properties
src/main/resources/static/index.html
compose.yml
```

---

## 3. Explication des fichiers

### 3.1 `PageEvent.java` (Event Model)

```java
public record PageEvent(String name, String user, Date date, long duration) { }
```

* **But** : représenter un événement de page.
* `name` → nom de la page (ex : P1 ou P2)
* `user` → l’utilisateur (U1 ou U2)
* `date` → date de l’événement
* `duration` → durée passée sur la page en millisecondes

> `record` est une classe immuable introduite en Java 16.

---

### 3.2 `PageEventHandler.java` (Traitement des événements)

```java
@Bean
public Consumer<PageEvent> pageEventConsumer(){ ... }
```

* **Consumer** : reçoit les événements Kafka et les affiche dans la console.

```java
@Bean
public Supplier<PageEvent> pageEventSupplier(){ ... }
```

* **Supplier** : produit des événements aléatoires toutes les x millisecondes et les envoie à Kafka.

```java
@Bean
public Function<KStream<String, PageEvent>, KStream<String, Long>> KStreamFunction(){ ... }
```

* **Function** : Kafka Stream qui :

    1. Filtre les événements avec `duration > 100ms`
    2. Mappe chaque événement pour ne garder que `(name, duration)`
    3. Regroupe par clé (nom de la page)
    4. Crée des **fenêtres de 5 secondes** (`TimeWindows`)
    5. Compte le nombre d’événements par page
    6. Transforme le résultat pour renvoyer `(name, count)`

---

### 3.3 `PageEventController.java` (REST Controller)

```java
@GetMapping("/publish")
public PageEvent publish(String name, String topic) { ... }
```

* Permet de **publier un événement manuellement** via URL :

  ```
  http://localhost:8084/publish?name=P1&topic=T3
  ```

```java
@GetMapping(path = "/analytics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<Map<String, Long>> analytics() { ... }
```

* Flux SSE (`Server-Sent Events`) qui **envoie les analytics en temps réel** au frontend toutes les secondes.
* Lit les données depuis le **store Kafka Streams** nommé `count-store`.

---

### 3.4 `index.html` (Frontend)

* Utilise **Smoothie Charts** pour afficher un graphique en temps réel.
* Se connecte au endpoint `/analytics` via **EventSource**.
* Affiche les pages P1 et P2 avec des couleurs différentes.
* Chaque seconde, récupère les données et met à jour le graphique.

> ⚠️ Screenshot recommandé : le graphique en live dans le navigateur.

---

### 3.5 `application.properties`

```properties
spring.application.name=kafka-spring-cloud-steam
server.port=8084

# Bindings Kafka
spring.cloud.stream.bindings.pageEventConsumer-in-0.destination=T2
spring.cloud.stream.bindings.pageEventSupplier-out-0.destination=T3
spring.cloud.stream.bindings.KStreamFunction-in-0.destination=T3
spring.cloud.stream.bindings.KStreamFunction-out-0.destination=T4

spring.cloud.function.definition=pageEventConsumer;pageEventSupplier;KStreamFunction
spring.cloud.stream.bindings.pageEventSupplier-out-0.producer.poller.fixed-delay.=200
spring.cloud.stream.kafka.binder.configuration.commit.interval.ms=1000
```

* **Destination** → le topic Kafka associé
* `pageEventConsumer-in-0` → consomme depuis le topic `T2`
* `pageEventSupplier-out-0` → produit vers `T3`
* `KStreamFunction` → lit depuis `T3`, écrit dans `T4`
* `commit.interval.ms` → intervalle de commit Kafka

---

### 3.6 `compose.yml`

* Contient la configuration **Docker** pour Kafka et Zookeeper
* Permet de **lancer les services Kafka en local**

> Screenshot recommandé : `docker ps` montrant les conteneurs Kafka et Zookeeper.

---

## 4. Commandes à exécuter

1. **Lancer Docker Compose** :

```bash
docker-compose up -d
```

2. **Vérifier les conteneurs** :

```bash
docker ps
```

3. **Lancer l’application Spring Boot** depuis IDE ou :

```bash
mvn spring-boot:run
```

4. **Tester le publish manuel** :

```
http://localhost:8084/publish?name=P1&topic=T3
```

5. **Voir les analytics en temps réel** :

```
http://localhost:8084/index.html
```

---

## 5. Screenshots 

1. Docker containers (`docker ps`)
2. Frontend graphique en temps réel (`index.html`)
3. Console Spring Boot affichant les événements consommés
4. Kafka topics (optionnel) :

---

## 6. Conseils pour comprendre le code

* **Consumer / Supplier / Function** = cœur de Spring Cloud Stream
* **Flux & SSE** → permet d’envoyer des données en **temps réel** au navigateur
* **Windowed store** → permet de compter les événements dans des fenêtres temporelles (ex : 5 secondes)

---

