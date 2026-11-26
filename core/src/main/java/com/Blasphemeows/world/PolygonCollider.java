package com.Blasphemeows.world;

import com.Blasphemeows.player.Player;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.utils.ShortArray;

/**
 * Resolves 2D AABB player collision against an arbitrary (convex or concave) Tiled polygon.
 * Supports sloped walkable surfaces, vertical walls, ceilings, and one-way platforms.
 */
public class PolygonCollider {
    private static final float MAX_WALKABLE_SLOPE = 2.5f;
    private static final float SURFACE_TOLERANCE = 28f;
    private static final float LAND_TOLERANCE = 12f;
    private static final float STEP_UP_SLOPE_MARGIN = 18f;

    private final float[] vertices;
    private final float[] triangles;
    private final boolean oneWay;
    private final float minX;
    private final float maxX;
    private final float minY;
    private final float maxY;

    /**
     * @param worldVertices Flat x,y pairs in libGDX Y-up world coordinates (at least 3 vertices).
     * @param oneWay        True if the player can jump up through the underside and drop down.
     */
    public PolygonCollider(float[] worldVertices, boolean oneWay) {
        if (worldVertices == null || worldVertices.length < 6 || worldVertices.length % 2 != 0) {
            throw new IllegalArgumentException("PolygonCollider requires at least 3 x/y vertices");
        }
        this.vertices = ensureCounterClockwise(worldVertices.clone());
        this.oneWay = oneWay;

        float x0 = Float.POSITIVE_INFINITY, x1 = Float.NEGATIVE_INFINITY;
        float y0 = Float.POSITIVE_INFINITY, y1 = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < this.vertices.length; i += 2) {
            x0 = Math.min(x0, this.vertices[i]);
            x1 = Math.max(x1, this.vertices[i]);
            y0 = Math.min(y0, this.vertices[i + 1]);
            y1 = Math.max(y1, this.vertices[i + 1]);
        }
        this.minX = x0;
        this.maxX = x1;
        this.minY = y0;
        this.maxY = y1;

        ShortArray idx = new EarClippingTriangulator().computeTriangles(this.vertices);
        this.triangles = new float[idx.size * 2];
        for (int i = 0; i < idx.size; i++) {
            int vi = idx.get(i) * 2;
            this.triangles[i * 2] = this.vertices[vi];
            this.triangles[i * 2 + 1] = this.vertices[vi + 1];
        }
    }

    /** Creates a rectangular PolygonCollider in libGDX Y-up world coordinates. */
    public static PolygonCollider fromRectangle(float x, float y, float width, float height, boolean oneWay) {
        return new PolygonCollider(new float[] {
            x, y,
            x + width, y,
            x + width, y + height,
            x, y + height
        }, oneWay);
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public float[] getVertices() {
        return vertices;
    }

    public float getMinX() { return minX; }
    public float getMaxX() { return maxX; }
    public float getMinY() { return minY; }
    public float getMaxY() { return maxY; }

    /** Resolves collision between the player and this polygon. */
    public boolean resolveCollision(Player player, float previousX, float previousY) {
        float px = player.getX();
        float py = player.getY();
        float pw = player.getWidth();
        float ph = player.getHeight();

        // Broad-phase AABB check (with small margin for slope tracking)
        if (px + pw < minX - 2f || px > maxX + 2f || py + ph < minY - 2f || py > maxY + SURFACE_TOLERANCE) {
            return false;
        }

        if (oneWay) {
            return resolveOneWay(player, previousX, previousY);
        }

        boolean resolved = resolveWalkableSurface(player, previousX, previousY);
        if (resolveSolidTriangles(player, previousX, previousY)) {
            resolved = true;
        }
        return resolved;
    }

    private boolean resolveOneWay(Player player, float previousX, float previousY) {
        if (player.isDroppingThroughPlatform()) {
            return false;
        }

        float px = player.getX();
        float py = player.getY();
        float pw = player.getWidth();
        float centerX = px + pw * 0.5f;
        float prevCenterX = previousX + pw * 0.5f;

        float surfaceY = getTopSurfaceY(centerX, px + 4f, px + pw - 4f, py + LAND_TOLERANCE);
        if (Float.isNaN(surfaceY)) {
            return false;
        }

        boolean fallingOntoSurface = previousY >= surfaceY - LAND_TOLERANCE
                && py <= surfaceY
                && player.getVelocityY() <= 0f;
        if (fallingOntoSurface) {
            player.setY(surfaceY);
            player.setVelocityY(0f);
            player.setGrounded(true);
            return true;
        }

        float prevSurfaceY = getTopSurfaceY(prevCenterX, previousX + 4f, previousX + pw - 4f, previousY + SURFACE_TOLERANCE);
        boolean movingHorizontally = Math.abs(centerX - prevCenterX) > 0.001f;
        boolean nearPrevSurface = !Float.isNaN(prevSurfaceY)
                && Math.abs(previousY - prevSurfaceY) <= SURFACE_TOLERANCE;
        if (nearPrevSurface && player.getVelocityY() <= 0f && (movingHorizontally || py <= surfaceY + 2f)) {
            player.setY(surfaceY);
            player.setVelocityY(0f);
            player.setGrounded(true);
            return true;
        }

        return false;
    }

    private boolean resolveWalkableSurface(Player player, float previousX, float previousY) {
        float px = player.getX();
        float py = player.getY();
        float pw = player.getWidth();
        float centerX = px + pw * 0.5f;
        float prevCenterX = previousX + pw * 0.5f;

        float surfaceY = getTopSurfaceY(centerX, px + 4f, px + pw - 4f, py + SURFACE_TOLERANCE);
        if (Float.isNaN(surfaceY)) {
            return false;
        }

        if (previousY >= surfaceY - LAND_TOLERANCE && py <= surfaceY && player.getVelocityY() <= 0f) {
            player.setY(surfaceY);
            player.setVelocityY(0f);
            player.setGrounded(true);
            return true;
        }

        float prevSurfaceY = getTopSurfaceY(prevCenterX, previousX + 4f, previousX + pw - 4f, previousY + SURFACE_TOLERANCE);
        boolean movingHorizontally = Math.abs(centerX - prevCenterX) > 0.001f;
        boolean nearPrevSurface = !Float.isNaN(prevSurfaceY)
                && Math.abs(previousY - prevSurfaceY) <= SURFACE_TOLERANCE;
        if (nearPrevSurface && player.getVelocityY() <= 0f && (movingHorizontally || py <= surfaceY + 2f)) {
            player.setY(surfaceY);
            player.setVelocityY(0f);
            player.setGrounded(true);
            return true;
        }

        return false;
    }

    /**
     * Resolves solid wall and ceiling collisions using SAT against the polygon's triangulation.
     * Skips small horizontal pushes caused by the player's feet resting on a walkable slope.
     */
    private boolean resolveSolidTriangles(Player player, float previousX, float previousY) {
        boolean anyResolved = false;

        for (int i = 0; i + 5 < triangles.length; i += 6) {
            float ax = triangles[i], ay = triangles[i + 1];
            float bx = triangles[i + 2], by = triangles[i + 3];
            float cx = triangles[i + 4], cy = triangles[i + 5];

            float px = player.getX();
            float py = player.getY();
            float pw = player.getWidth();
            float ph = player.getHeight();

            float[] mtv = computeBoxTriangleMTV(px, py, pw, ph, ax, ay, bx, by, cx, cy);
            if (mtv == null) {
                continue;
            }

            float pushX = mtv[0];
            float pushY = mtv[1];

            // Ignore tiny horizontal pushes at foot level when standing/walking on a walkable slope
            float triMaxY = Math.max(ay, Math.max(by, cy));
            if (Math.abs(pushX) > 0f && Math.abs(pushY) < 0.001f
                    && player.isGrounded() && triMaxY <= py + STEP_UP_SLOPE_MARGIN) {
                continue;
            }

            if (Math.abs(pushX) > 0f) {
                player.setX(px + pushX);
                player.setVelocityX(0f);
                anyResolved = true;
            }
            if (Math.abs(pushY) > 0f) {
                player.setY(py + pushY);
                if (pushY > 0f && player.getVelocityY() <= 0f) {
                    player.setVelocityY(0f);
                    player.setGrounded(true);
                } else if (pushY < 0f && player.getVelocityY() > 0f) {
                    player.setVelocityY(0f);
                }
                anyResolved = true;
            }
        }
        return anyResolved;
    }

    /**
     * Finds the highest walkable upward-facing edge under the player's center or foot edges
     * that is at or below {@code maxAllowedY}.
     */
    private float getTopSurfaceY(float centerX, float leftFootX, float rightFootX, float maxAllowedY) {
        float yCenter = sampleWalkableSurfaceAt(centerX, maxAllowedY);
        if (!Float.isNaN(yCenter)) {
            return yCenter;
        }
        float yLeft = sampleWalkableSurfaceAt(leftFootX, maxAllowedY);
        float yRight = sampleWalkableSurfaceAt(rightFootX, maxAllowedY);
        if (Float.isNaN(yLeft)) return yRight;
        if (Float.isNaN(yRight)) return yLeft;
        return Math.max(yLeft, yRight);
    }

    private float sampleWalkableSurfaceAt(float x, float maxAllowedY) {
        float bestY = Float.NaN;
        int n = vertices.length;
        for (int i = 0; i < n; i += 2) {
            float x1 = vertices[i];
            float y1 = vertices[i + 1];
            int next = (i + 2) % n;
            float x2 = vertices[next];
            float y2 = vertices[next + 1];

            // For a CCW polygon in Y-up, upward-facing edges run right-to-left (x2 < x1)
            float dx = x2 - x1;
            if (dx >= -0.001f) {
                continue;
            }
            float slope = Math.abs((y2 - y1) / dx);
            if (slope > MAX_WALKABLE_SLOPE) {
                continue;
            }
            if (x >= x2 && x <= x1) {
                float t = (x - x1) / dx;
                float y = y1 + t * (y2 - y1);
                if (y <= maxAllowedY && (Float.isNaN(bestY) || y > bestY)) {
                    bestY = y;
                }
            }
        }
        return bestY;
    }

    /**
     * Separating Axis Theorem between an AABB and a triangle.
     * Returns the minimum translation vector {dx, dy} to push the AABB out of the triangle, or null if separated.
     */
    private static float[] computeBoxTriangleMTV(
            float bx, float by, float bw, float bh,
            float x1, float y1, float x2, float y2, float x3, float y3) {

        float minOverlap = Float.POSITIVE_INFINITY;
        float bestAxisX = 0f;
        float bestAxisY = 0f;

        // 2 AABB axes + 3 triangle edge normals
        float[] axesX = { 1f, 0f, -(y2 - y1), -(y3 - y2), -(y1 - y3) };
        float[] axesY = { 0f, 1f,  (x2 - x1),  (x3 - x2),  (x1 - x3) };

        float boxCenterX = bx + bw * 0.5f;
        float boxCenterY = by + bh * 0.5f;
        float triCenterX = (x1 + x2 + x3) / 3f;
        float triCenterY = (y1 + y2 + y3) / 3f;

        for (int a = 0; a < 5; a++) {
            float ax = axesX[a];
            float ay = axesY[a];
            float len = (float) Math.sqrt(ax * ax + ay * ay);
            if (len < 1e-6f) continue;
            ax /= len;
            ay /= len;

            // Project AABB
            float rBox = (bw * 0.5f) * Math.abs(ax) + (bh * 0.5f) * Math.abs(ay);
            float cBox = boxCenterX * ax + boxCenterY * ay;
            float boxMin = cBox - rBox;
            float boxMax = cBox + rBox;

            // Project Triangle
            float p1 = x1 * ax + y1 * ay;
            float p2 = x2 * ax + y2 * ay;
            float p3 = x3 * ax + y3 * ay;
            float triMin = Math.min(p1, Math.min(p2, p3));
            float triMax = Math.max(p1, Math.max(p2, p3));

            float overlap = Math.min(boxMax, triMax) - Math.max(boxMin, triMin);
            if (overlap <= 0.0001f) {
                return null; // Separating axis found
            }

            if (overlap < minOverlap) {
                minOverlap = overlap;
                // Ensure axis points from triangle toward box
                float dir = (boxCenterX - triCenterX) * ax + (boxCenterY - triCenterY) * ay;
                if (dir < 0f) {
                    bestAxisX = -ax;
                    bestAxisY = -ay;
                } else {
                    bestAxisX = ax;
                    bestAxisY = ay;
                }
            }
        }

        // Prefer pure vertical or pure horizontal resolution when near-axis-aligned
        if (Math.abs(bestAxisX) > Math.abs(bestAxisY) * 2f) {
            return new float[] { Math.signum(bestAxisX) * minOverlap, 0f };
        }
        if (Math.abs(bestAxisY) > Math.abs(bestAxisX) * 2f) {
            return new float[] { 0f, Math.signum(bestAxisY) * minOverlap };
        }
        return new float[] { bestAxisX * minOverlap, bestAxisY * minOverlap };
    }

    private static float[] ensureCounterClockwise(float[] pts) {
        double area = 0.0;
        int n = pts.length;
        for (int i = 0; i < n; i += 2) {
            int next = (i + 2) % n;
            area += (double) pts[i] * pts[next + 1] - (double) pts[next] * pts[i + 1];
        }
        if (area < 0.0) {
            // Reverse vertex order in-place
            for (int i = 0, j = n - 2; i < j; i += 2, j -= 2) {
                float tx = pts[i];
                float ty = pts[i + 1];
                pts[i] = pts[j];
                pts[i + 1] = pts[j + 1];
                pts[j] = tx;
                pts[j + 1] = ty;
            }
        }
        return pts;
    }
}
