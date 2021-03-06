package battlecode.client.viewer.sound;

import battlecode.client.viewer.GameStateTimeline;

import java.util.Observable;
import java.util.Observer;

public class AudioPlayer {

    private AudioTimeline timeline;
    private PlayState playState = new PlayState();

    public void setTimeline(GameStateTimeline gst) {
        timeline = new AudioTimeline(gst);
        timeline.setTargetState(playState);
        timeline.addObserver((o, arg) -> playState.play());
    }
}
