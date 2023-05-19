package framework.telegram.ui.cameraview;

import framework.telegram.ui.cameraview.options.Facing;
import framework.telegram.ui.cameraview.options.Flash;
import framework.telegram.ui.cameraview.options.Hdr;
import framework.telegram.ui.cameraview.options.WhiteBalance;

abstract class Mapper {

    abstract <T> T map(Flash flash);
    abstract <T> T map(Facing facing);
    abstract <T> T map(WhiteBalance whiteBalance);
    abstract <T> T map(Hdr hdr);
    abstract <T> Flash unmapFlash(T cameraConstant);
    abstract <T> Facing unmapFacing(T cameraConstant);
    abstract <T> WhiteBalance unmapWhiteBalance(T cameraConstant);
    abstract <T> Hdr unmapHdr(T cameraConstant);
}
