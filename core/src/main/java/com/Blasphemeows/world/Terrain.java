package com.Blasphemeows.world;

import com.Blasphemeows.player.Player;

/** A filled ground formation with a piecewise-linear, walkable top surface. */
public class Terrain {
    /** Allow ~68° ramps (hub canyon / dais approaches) before treating as a wall. */
    private static final float MAX_WALKABLE_SLOPE = 2.5f;
    private static final float SURFACE_TOLERANCE = 28f;
    private static final float LAND_TOLERANCE = 12f;
    private final float[] points;

    /** Points are x/y pairs, ordered from left to right along the upper contour. */
    public Terrain(float... points) {
        if (points == null || points.length < 4 || points.length % 2 != 0) {
            throw new IllegalArgumentException("Terrain needs at least two x/y points");
        }
        for (int i = 2; i < points.length; i += 2) {
            if (points[i] <= points[i - 2]) {
                throw new IllegalArgumentException("Terrain x coordinates must increase");
            }
        }
        this.points = points.clone();
    }

    public float[] getPoints() {
        return points.clone();
    }

    /**
     * Resolves landing and uphill traversal against the sampled top edge.
     * Cost is linear in the small number of contour segments in this terrain.
     */
    public boolean resolveCollision(Player player, float previousY) {
        return resolveCollision(player, player.getX(), previousY);
    }

    public boolean resolveCollision(Player player, float previousX, float previousY) {
        // Solid ground always collides — drop-through is for platforms / one-way cliffs only.
        float centerX = player.getX() + player.getWidth() * 0.5f;
        float previousCenterX = previousX + player.getWidth() * 0.5f;
        float surfaceY = getSurfaceY(centerX);
        if (Float.isNaN(surfaceY)) return false;

        float y = player.getY();
        float slope = getSlope(centerX);
        boolean walkable = Math.abs(slope) <= MAX_WALKABLE_SLOPE;

        boolean fallingOntoSurface = previousY >= surfaceY - LAND_TOLERANCE && y <= surfaceY
                && player.getVelocityY() <= 0f;
        if (fallingOntoSurface && walkable) {
            player.setY(surfaceY);
            player.setVelocityY(0f);
            player.setGrounded(true);
            return true;
        }

        float previousSurfaceY = getSurfaceY(previousCenterX);
        boolean movingHorizontally = Math.abs(centerX - previousCenterX) > 0.001f;
        boolean nearPreviousSurface = !Float.isNaN(previousSurfaceY)
                && Math.abs(previousY - previousSurfaceY) <= SURFACE_TOLERANCE;
        boolean continuingAlongSurface = nearPreviousSurface && player.getVelocityY() <= 0f
                && (movingHorizontally || y <= surfaceY + 2f);

        if (continuingAlongSurface) {
            if (walkable) {
                // Follow the contour exactly so steep ramps feel continuous.
                player.setY(surfaceY);
                player.setVelocityY(0f);
                player.setGrounded(true);
                return true;
            }
            // Near-vertical face: block horizontal progress instead of climbing a wall.
            player.setX(previousX);
            player.setVelocityX(0f);
            player.setVelocityY(0f);
            player.setGrounded(true);
            return true;
        }

        // Filled body is solid: block walking into a cliff face from the side.
        if (movingHorizontally && y < surfaceY - SURFACE_TOLERANCE
                && previousY < surfaceY - SURFACE_TOLERANCE) {
            player.setX(previousX);
            player.setVelocityX(0f);
            return true;
        }
        return false;
    }

    private float getSurfaceY(float x) {
        for (int i = 0; i < points.length - 2; i += 2) {
            float x1 = points[i];
            float x2 = points[i + 2];
            if (x >= x1 && x <= x2) {
                float t = (x - x1) / (x2 - x1);
                return points[i + 1] + t * (points[i + 3] - points[i + 1]);
            }
        }
        return Float.NaN;
    }

    private float getSlope(float x) {
        for (int i = 0; i < points.length - 2; i += 2) {
            float x1 = points[i];
            float x2 = points[i + 2];
            if (x >= x1 && x <= x2) {
                return (points[i + 3] - points[i + 1]) / (x2 - x1);
            }
        }
        return 0f;
    }
}
