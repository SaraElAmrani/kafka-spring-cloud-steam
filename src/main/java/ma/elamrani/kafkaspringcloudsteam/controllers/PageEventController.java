package ma.elamrani.kafkaspringcloudsteam.controllers;

import ma.elamrani.kafkaspringcloudsteam.events.PageEvent;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Ce contrôleur expose 2 endpoints HTTP :
 *
 * 1. GET /publish?name=P1&topic=T2
 *    → Permet de publier manuellement un événement vers un topic Kafka
 *    → Utile pour tester l'application
 *
 * 2. GET /analytics
 *    → Envoie les statistiques en temps réel au client web
 *    → Utilise Server-Sent Events (SSE)
 *    → Le navigateur reçoit des mises à jour automatiques chaque seconde
 *
 * RESPONSABILITÉS :
 * - Interface entre l'utilisateur (navigateur) et Kafka
 * - Publication manuelle d'événements
 * - Lecture du State Store de Kafka Streams
 * - Streaming des résultats vers le client web
 */

@RestController  // Indique à Spring que c'est un contrôleur REST (retourne du JSON/données)
public class PageEventController {

    // INJECTION DE DÉPENDANCES
    /**
     * StreamBridge : Permet d'envoyer des messages vers Kafka
     *
     * UTILITÉ :
     * - Envoyer manuellement des événements vers n'importe quel topic
     * - Alternative à l'utilisation d'un Supplier
     * - Utile pour des publications ponctuelles (pas automatiques)
     *
     * EXEMPLE D'UTILISATION :
     * streamBridge.send("nom-du-topic", monObjet);
     */

    @Autowired // Spring injecte automatiquement cette dépendance
    private StreamBridge streamBridge;
    @Autowired
    private InteractiveQueryService interactiveQueryService;

    @GetMapping("/publish")
    public PageEvent publish(String name, String topic) {
        PageEvent event = new PageEvent(
                name,
                Math.random()>0.5?"U1":"U2",
                new Date(),10+new Random().nextInt(10000));
        streamBridge.send(topic, event);
        return event;
    }

    @GetMapping(path = "/analytics",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics(){
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence->{
                    Map<String,Long> stringLongMap=new HashMap<>();
                    ReadOnlyWindowStore<String, Long> windowStore = interactiveQueryService.getQueryableStore("count-store", QueryableStoreTypes.windowStore());
                    Instant now=Instant.now();
                    Instant from=now.minusMillis(5000);
                    KeyValueIterator<Windowed<String>, Long> fetchAll = windowStore.fetchAll(from, now);
                    //WindowStoreIterator<Long> fetchAll = windowStore.fetch(page, from, now);
                    while (fetchAll.hasNext()){
                        KeyValue<Windowed<String>, Long> next = fetchAll.next();
                        stringLongMap.put(next.key.key(),next.value);
                    }
                    return stringLongMap;
                });
    }


}
