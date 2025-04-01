package com.cavetale.hive;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

@Data
public final class Orbit {
    private Location center;
    private ItemStack item;
    private ItemDisplay itemDisplay;
    private Vector rotationAngle;
    private double time;
    private double distance;
    private boolean diminish;

    public Orbit(final Location center, final ItemStack item) {
        this.center = center;
        this.item = item;
        rotationAngle = Vector.getRandom().normalize().multiply(PI * 2);
        itemDisplay = center.getWorld().spawn(getCurrentLocation(), ItemDisplay.class, e -> {
                e.setPersistent(false);
                e.setItemStack(item);
                e.setBillboard(ItemDisplay.Billboard.CENTER);
                final Vector3f translation = new Vector3f(0f, 0f, 0f);
                final AxisAngle4f leftRotation = new AxisAngle4f(0f, 0f, 0f, 0f);
                final AxisAngle4f rightRotation = new AxisAngle4f((float) PI, 0f, 1f, 0f);
                final float scalef = 0.5f;
                final Vector3f scale = new Vector3f(scalef, scalef, scalef);
                e.setTransformation(new Transformation(translation, leftRotation, scale, rightRotation));
            });
    }

    public void remove() {
        itemDisplay.remove();
        itemDisplay = null;
    }

    public void setItem(ItemStack theItem) {
        this.item = theItem;
        if (itemDisplay != null) {
            itemDisplay.setItemStack(item);
        }
    }

    public boolean isDead() {
        return itemDisplay == null;
    }

    public void onTick() {
        if (!diminish) {
            if (distance < 1.0) {
                distance += 0.01;
            }
        } else {
            if (distance > 0.01) {
                distance -= 0.01;
            } else {
                remove();
                return;
            }
        }
        time += 0.15;
        itemDisplay.teleport(getCurrentLocation());
    }

    private Location getCurrentLocation() {
        final Vector vector = new Vector(cos(time) * distance,
                                         0.0,
                                         sin(time) * distance);
        final Vector angle = rotationAngle;
        double x;
        double y;
        double z;
        // Z
        x = vector.getX() * cos(angle.getZ()) - vector.getY() * sin(angle.getZ());
        y = vector.getX() * sin(angle.getZ()) + vector.getY() * cos(angle.getZ());
        z = vector.getZ();
        vector.setX(x);
        vector.setY(y);
        vector.setZ(z);
        // Y
        z = vector.getZ() * cos(angle.getY()) - vector.getX() * sin(angle.getY());
        x = vector.getZ() * sin(angle.getY()) + vector.getX() * cos(angle.getY());
        y = vector.getY();
        vector.setX(x);
        vector.setY(y);
        vector.setZ(z);
        // X
        y = vector.getY() * cos(angle.getX()) - vector.getZ() * sin(angle.getX());
        z = vector.getY() * sin(angle.getX()) + vector.getZ() * cos(angle.getX());
        x = vector.getX();
        vector.setX(x);
        vector.setY(y);
        vector.setZ(z);
        final Location location = center.clone().add(vector);
        location.setYaw((float) time * 100f);
        return location;
    }
}
