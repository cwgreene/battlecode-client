package battlecode.client.viewer.render;

import battlecode.common.MapLocation;

class DoodadAnimation extends FramedAnimation {
    public enum DoodadType {
        EXPLOSION("art/explode/explode64_f%02d.png", 1, 1, 9);

        public final String formatString;
        public final int w;
        public final int h;
        public final int frameCount;


        DoodadType(String formatString, int w, int h, int frameCount) {
            this.formatString = formatString;
            this.w = w;
            this.h = h;
            this.frameCount = frameCount;
        }
    }

    public DoodadType type;

    public DoodadAnimation(MapLocation loc) {
        this(loc, 1, DoodadType.EXPLOSION);
    }

    public DoodadAnimation(MapLocation loc, double width, DoodadType type) {
        super(loc, width * type.w, type.frameCount);
        this.type = type;
    }

    protected boolean loops() {
        return true;
    }

    int offset() {
        return 0;
    }

    public String fileFormatString() {
        return type.formatString;
    }

    protected boolean shouldDraw() {
        return true;
    }

    public Object clone() {
        DoodadAnimation clone = new DoodadAnimation(loc, width, type);
        clone.curFrame = curFrame;
        return clone;
    }
}
