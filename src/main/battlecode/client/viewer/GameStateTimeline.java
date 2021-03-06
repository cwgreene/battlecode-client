package battlecode.client.viewer;

import battlecode.world.signal.InternalSignal;
import battlecode.serial.RoundDelta;

import java.util.*;

public class GameStateTimeline<E extends GameState> extends Observable {

    private GameStateFactory<E> gsf;
    private List<E> keyFrames;
    private final int roundsPerKey;

    protected BufferedMatch match;
    private boolean active = false; // technically volatile, but we can be
    // lenient

    protected E currentState = null;
    protected int currentRound = -1;

    private E currentStateAlignedClone = null;
    private List<InternalSignal> debugInternalSignals = null;
    private int appliedDebugSignals;

    private volatile int roundsProcessed = -1;

    private volatile long cloneTime = 0;
    private volatile long numClones = 0;
    private volatile long applyTime = 0;
    private volatile long numApplies = 0;

    public GameStateTimeline(BufferedMatch match, GameStateFactory<E> gsf,
                             int rpk) {
        this.gsf = gsf;
        this.roundsPerKey = rpk;
        this.match = match;
        match.addMatchListener(new MatchListener() {
            public void headerReceived(BufferedMatch m) {
                keyFrames = new ArrayList<>(1 + m.getHeader().getMap()
                        .getRounds() /
                        roundsPerKey);
                active = true;
                (new Thread() {
                    public void run() {
                        createKeyFrames();
                    }
                }).start();
            }
        });
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return match.isFinished() && roundsProcessed >= match
                .getRoundsAvailable();
    }

    public int getRoundsPerKey() {
        return roundsPerKey;
    }

    /**
     * Stops the creation of keyframes for this timeline and notifies all
     * Observers.
     * Should only be used to dispose of the match viewer and player.
     */
    public void terminate() {
        active = false;
        setChanged();
        notifyObservers();
        deleteObservers();
    }

    @SuppressWarnings("unchecked")
    protected void createKeyFrames() {
        E gs = gsf.createState(match.getHeader().getMap());
        keyFrames.add(cloneState(gs));
        roundsProcessed = 0;
        while (active) {
            int roundsAvailable = match.getRoundsAvailable();
            if (roundsProcessed == roundsAvailable && match.isFinished()) {
                break;
            }
            while (roundsProcessed < roundsAvailable && active) {
                RoundDelta delta = match.getRound(roundsProcessed);
                assert delta != null : "Null delta after handling " +
                        roundsProcessed + " rounds";
                applyDelta(gs, delta);
                if ((roundsProcessed + 1) % roundsPerKey == 0) {
                    keyFrames.add(cloneState(gs));
                }
                roundsProcessed++;
                synchronized (this) {
                    debugInternalSignals = null;
                }
            }
            appliedDebugSignals = 0;
            debugInternalSignals = match.getDebugSignals(roundsProcessed);
            if (debugInternalSignals != null) {
                try {
                    while (roundsProcessed == match.getRoundsAvailable()) {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                }
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private E cloneState(E gs) {
        long startTime = System.nanoTime();
        E clone = gsf.cloneState(gs);
        cloneTime += (System.nanoTime() - startTime);
        numClones++;
        return clone;
    }

    private void applyDelta(E gs, RoundDelta delta) {
        long startTime = System.nanoTime();
        gs.apply(delta);
        applyTime += (System.nanoTime() - startTime);
        numApplies++;
    }

    private synchronized void syncToDebugSignals() {
        if (debugInternalSignals != null) {
            if (currentStateAlignedClone == null) {
                currentStateAlignedClone = cloneState(currentState);
            }
            while (appliedDebugSignals < debugInternalSignals.size()) {
                currentState.apply(debugInternalSignals.get(appliedDebugSignals++));
                setChanged();
            }
        }
    }

    private boolean isKeyFrameRequired(int round) {
        if (numApplies == 0 || // we don't know about applies, so just do a
        // clone
                currentRound == -1 || // no delta available from -1 to 0
                round < currentRound || // need a key when rewinding
                round == 0) {
            return true;
        }
        int numDeltas = round - currentRound - (round % roundsPerKey);
        // suggest a copy if the extra delta applications would take more time
        return numDeltas * applyTime * numClones > cloneTime * numApplies;
    }

    public BufferedMatch getMatch() {
        return match;
    }

    public int getNumRounds() {
        return roundsProcessed;
    }

    public int getRound() {
        return currentRound;
    }

    public void setRound(int round) {
        if (round < 0) { // first, clamp the min
            round = 0;
        }
        if (round > roundsProcessed) {
            round = roundsProcessed;
        }
        if (currentState == null || round < 0) {
            return;
        }
        if (currentRound == round) {
            refreshRound();
            return;
        }

        if (isKeyFrameRequired(round)) {
            int keyFrame = round / roundsPerKey;
            gsf.copyState(keyFrames.get(keyFrame), currentState);
            currentRound = keyFrame * roundsPerKey;
        } else if (currentStateAlignedClone != null) { // realign currentState
            gsf.copyState(currentStateAlignedClone, currentState);
        }
        currentStateAlignedClone = null;

        while (currentRound < round) {
            applyDelta(currentState, match.getRound(currentRound));
            currentRound++;
        }
        setChanged();
        notifyObservers();
    }

    public void refreshRound() {
        if (roundsProcessed > 0) {
            syncToDebugSignals();
            notifyObservers();
        }
    }

    public void setTargetState(E state) {
        currentRound = -1;
        currentState = state;
    }

    public void setMasterTimeline(final GameStateTimeline gst) {
        gst.addObserver((o, arg) -> {
            if (gst.isActive()) {
                setRound(gst.getRound());
            } else {
                terminate();
            }
        });
    }

    protected void finalize() throws Throwable {
        //System.out.println("Clone time (ns): " + (cloneTime/numClones));
        //System.out.println("Apply time (ns): " + (applyTime/numApplies));
    }
}
