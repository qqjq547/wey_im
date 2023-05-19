package framework.telegram.support.system.event;

import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.processors.ReplayProcessor;

public class EventProcessor<T> {
    EventBus.EventType type = EventBus.EventType.Publish;

    public EventProcessor(EventBus.EventType type) {
        this.type = type;
        switch (type) {
            case Behavior:
                eventProcessor = (FlowableProcessor<T>) BehaviorProcessor.create().toSerialized();
                break;

            case Replay:
                eventProcessor = (FlowableProcessor<T>) ReplayProcessor.create().toSerialized();
                break;

            default:
                eventProcessor = (FlowableProcessor<T>) PublishProcessor.create().toSerialized();
        }
    }

    private FlowableProcessor<T> eventProcessor;

    public void process(T o) {
        try {
            eventProcessor.onNext(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Flowable<T> toFlowable() {
        return eventProcessor;
    }

}