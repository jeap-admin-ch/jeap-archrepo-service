package ch.admin.bit.jeap.archrepo.metamodel.message;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Entity
@Getter
public class MessageContract {

    @Id
    @NotNull
    @Getter
    private UUID id;

    @Setter
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_sender_id")
    private MessageType messageSender;

    @Setter
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_receiver_id")
    private MessageType messageReceiver;

    @Setter
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_publisher_id")
    private MessageType messagePublisher;

    @Setter
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_consumer_id")
    private MessageType messageConsumer;

    private String versions;
    private String componentName;
    private String topic;


    @Builder
    public MessageContract(List<String> version, String componentName, String topic) {
        this.id = UUID.randomUUID();
        this.versions = String.join(",", version);
        this.componentName = componentName;
        this.topic = topic;
    }

    public List<String> versionList(){
        return Arrays.stream(this.versions.split(",")).toList();
    }
}
