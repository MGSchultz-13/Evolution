package tgw.evolution.util.hitbox.hms;

public interface HM {

    void addRotationX(float dx);

    void addRotationY(float dy);

    void addRotationZ(float dz);

    default void copy(HM hm) {
        this.setRotationX(hm.xRot());
        this.setRotationY(hm.yRot());
        this.setRotationZ(hm.zRot());
        this.setPivotX(hm.getPivotX());
        this.setPivotY(hm.getPivotY());
        this.setPivotZ(hm.getPivotZ());
    }

    float getPivotX();

    float getPivotY();

    float getPivotZ();

    default void invertRotationY() {
        this.setRotationY(-this.yRot());
    }

    default void setPivot(float x, float y, float z) {
        this.setPivotX(x);
        this.setPivotY(y);
        this.setPivotZ(z);
    }

    void setPivotX(float x);

    void setPivotY(float y);

    void setPivotZ(float z);

    default void setRotation(float xRot, float yRot, float zRot) {
        this.setRotationX(xRot);
        this.setRotationY(yRot);
        this.setRotationZ(zRot);
    }

    void setRotationX(float rotX);

    void setRotationY(float rotY);

    void setRotationZ(float rotZ);

    void setVisible(boolean visible);

    void translateX(float x);

    void translateY(float y);

    void translateZ(float z);

    float xRot();

    float yRot();

    float zRot();
}
