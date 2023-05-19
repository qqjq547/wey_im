package framework.telegram.ui.face.dynamic;

public class DynamicFaceBean {

    public long id;

    public String name;

    public String path;

    public int width;

    public int height;

    public DynamicFaceBean(long id, String path, int width, int height) {
        this.id = id;
        this.path = path;
        this.width = width;
        this.height = height;
    }
}
