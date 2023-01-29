/*
 * Code from:  Mark-225/NeincraftPlugin  (https://github.com/Mark-225/NeincraftPlugin/blob/3a87dcd2f9c9d63a1ac43726f9bacd93c9f138cb/src/main/java/de/neincraft/neincraftplugin/modules/plots/util/PlotUtils.java)
 * License:    GNU General Public License v3.0  (https://github.com/Mark-225/NeincraftPlugin/blob/3a87dcd2f9c9d63a1ac43726f9bacd93c9f138cb/LICENSE)
 * Author:     Mark-225  (https://github.com/Mark-225)
 * Changes:    Class renamed, unused methods removed and support for non 16x16 chunks
 */

package codes.antti.bluemaptowny;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MapUtils {
    /**
     * Entrypoint for the polygon conversion algorithm
     * @param chunks the chunks to convert
     * @param areaPolygons a list of polygons to fill with area polygons (in chunk coordinates)
     * @param borderPolygons a list of polygons to fill with border polygons (in chunk coordinates)
     */
    public static void areaToPolygons(Set<Vector2i> chunks, List<List<Vector2i>> areaPolygons, List<List<Vector2i>> borderPolygons) {
        List<Set<Vector2i>> sectors = findSectors(chunks);
        sectors.forEach(sector -> traceArea(sector, areaPolygons, borderPolygons));
    }

    /**
     * Most user-friendly entry point. Converts a set of chunk coordinates to multiple area and border polygons.
     * @param chunks The set of chunk coordinates to convert.
     * @param areaPolygons The list to add the area polygons to.
     * @param borderPolygons The list to add the border polygons to.
     */
    public static void areaToBlockPolygon(Set<Vector2i> chunks, int blockSize, List<List<Vector2d>> areaPolygons, List<List<Vector2d>> borderPolygons){
        List<List<Vector2i>> areaChunkPolygons = new ArrayList<>();
        List<List<Vector2i>> borderChunkPolygons = new ArrayList<>();
        areaToPolygons(chunks, areaChunkPolygons, borderChunkPolygons);
        areaPolygons.addAll(areaChunkPolygons.stream().map(polygon -> polygon.stream().map(vector -> vector.mul(blockSize).toDouble()).collect(Collectors.toList())).collect(Collectors.toList()));
        borderPolygons.addAll(borderChunkPolygons.stream().map(polygon -> polygon.stream().map(vector -> vector.mul(blockSize).toDouble()).collect(Collectors.toList())).collect(Collectors.toList()));
    }

    /**
     * Creates border polygons and splits the area into two sectors if a hole is found. Then recursively calls itself for each sector.
     * @param chunks the sector to convert into polygons
     * @param areaPolygons the list of polygons that will be filled with the area polygons
     * @param borderPolygons the list of polygons that will be filled with the border polygons
     */
    private static void traceArea(Set<Vector2i> chunks, List<List<Vector2i>> areaPolygons, List<List<Vector2i>> borderPolygons){
        Set<Vector2i> westBorders = new HashSet<>();
        Set<Vector2i> southBorders = new HashSet<>();
        Set<Vector2i> northBorders = new HashSet<>();
        Set<Vector2i> eastBorders = new HashSet<>();

        chunks.forEach(chunk -> {
            if(!chunks.contains(chunk.add(-1, 0)))
                westBorders.add(chunk);
            if(!chunks.contains(chunk.add(1, 0)))
                eastBorders.add(chunk);
            if(!chunks.contains(chunk.add(0, 1)))
                southBorders.add(chunk);
            if(!chunks.contains(chunk.add(0, -1)))
                northBorders.add(chunk);
        });

        Map<Vector2i, Vector2i> westSegments = findSegments(westBorders, Vector2i.from(0, -1));
        Map<Vector2i, Vector2i> southSegments = findSegments(southBorders, Vector2i.from(-1, 0));
        Map<Vector2i, Vector2i> eastSegments = findSegments(eastBorders, Vector2i.from(0, 1));
        Map<Vector2i, Vector2i> northSegments = findSegments(northBorders, Vector2i.from(1, 0));

        Map<Direction, Map<Vector2i, Vector2i>> segments = Map.of(
                Direction.NORTH, new HashMap<>(northSegments),
                Direction.EAST, new HashMap<>(eastSegments),
                Direction.SOUTH, new HashMap<>(southSegments),
                Direction.WEST, new HashMap<>(westSegments));

        while(!segments.get(Direction.NORTH).isEmpty()) {
            Vector2i startVector = findNext(segments.get(Direction.NORTH).keySet());
            List<Vector2i> coordinates = new ArrayList<>();
            coordinates.add(startVector);
            coordinates.addAll(traceOneLoop(segments, startVector));
            borderPolygons.add(coordinates);
        }

        if(borderPolygons.size() > 1){
            int splitY = borderPolygons.get(1).get(0).getY();
            Set<Vector2i> northChunks = chunks.stream().filter(chunk -> chunk.getY() < splitY).collect(Collectors.toSet());
            Set<Vector2i> southChunks = chunks.stream().filter(chunk -> chunk.getY() >= splitY).collect(Collectors.toSet());
            for(Set<Vector2i> northSector : findSectors(northChunks)){
                traceArea(northSector, areaPolygons, new ArrayList<>());
            }
            for(Set<Vector2i> southSector : findSectors(southChunks)){
                traceArea(southSector, areaPolygons, new ArrayList<>());
            }
        }else if(!borderPolygons.isEmpty()){
            areaPolygons.add(borderPolygons.get(0));
        }
    }

    /**
     * Creates a polygon by tracing border vectors until it reaches the starting vector again.
     * @param segments the border segments (represented by vector pairs) to trace
     * @param start the starting vector
     * @return A list of vectors representing the polygon
     */
    private static List<Vector2i> traceOneLoop(Map<Direction, Map<Vector2i, Vector2i>> segments, Vector2i start){
        List<Vector2i> coordinates = new ArrayList<>();
        Direction currentDirection = Direction.NORTH;
        Vector2i currentTarget;
        Vector2i currentStart = start;
        while((currentTarget = segments.get(currentDirection).get(currentStart)) != null){
            Vector2i coordinateTarget = currentTarget.add(currentDirection == Direction.NORTH || currentDirection == Direction.EAST ? 1 : 0, currentDirection == Direction.EAST || currentDirection == Direction.SOUTH ? 1 : 0);
            coordinates.add(coordinateTarget);
            segments.get(currentDirection).remove(currentStart);
            if(segments.get(currentDirection.getPrimary()).containsKey(currentTarget)){
                currentDirection = currentDirection.getPrimary();
                currentStart = currentTarget;
            }else if(segments.get(currentDirection.getSecondary()).containsKey(currentTarget.add(currentDirection.getSecondaryOffset()))){
                currentStart = currentTarget.add(currentDirection.getSecondaryOffset());
                currentDirection = currentDirection.getSecondary();
            }else{
                break;
            }
        }
        return coordinates;
    }

    /**
     * Splits a set of chunks into sectors using breadth-first search. A sector is a subset of chunks in which each chunk can be reached by every other chunk using only straight lines.
     * @param chunks The chunks to split into sectors.
     * @return A list of sectors.
     */
    private static List<Set<Vector2i>> findSectors(Set<Vector2i> chunks){
        List<Set<Vector2i>> sectors = new ArrayList<>();
        while (!chunks.isEmpty()){
            Set<Vector2i> sector = new HashSet<>();
            boolean changed = true;
            Set<Vector2i> searchSources = new HashSet<>();
            Vector2i firstChunk = chunks.iterator().next();
            sector.add(firstChunk);
            searchSources.add(firstChunk);
            while(changed){
                changed = false;
                Set<Vector2i> addedChunks = new HashSet<>();
                for(Vector2i chunk : searchSources){
                    addedChunks.addAll(Stream.of(chunk.add(0, -1),
                                    chunk.add(0, 1),
                                    chunk.add(1, 0),
                                    chunk.add(-1, 0))
                            .filter(c -> !sector.contains(c) && chunks.contains(c)).collect(Collectors.toCollection(HashSet::new)));
                }
                if(!addedChunks.isEmpty()){
                    changed = true;
                    searchSources = addedChunks;
                    sector.addAll(addedChunks);
                }
            }
            sectors.add(sector);
            chunks.removeAll(sector);
        }
        return sectors;
    }

    /**
     * Finds the most north-west vector in a set of vectors.
     * Ensures some predictability for the order in which the algorithm operates
     * @param vectors the set of vectors
     */
    private static Vector2i findNext(Set<Vector2i> vectors){
        Vector2i min = null;
        for(Vector2i cur : vectors){
            if(min == null || cur.getY() < min.getY()|| (cur.getY() == min.getY() && cur.getX() < min.getX())){
                min = cur;
            }
        }
        return min;
    }

    /**
     * Creates border vector pairs from a collection of border chunks
     * @param borders the collection of border chunks
     * @param forwardOffset the vector that defines the "forward" direction for the given border chunks
     */
    private static Map<Vector2i, Vector2i> findSegments(Set<Vector2i> borders, Vector2i forwardOffset){
        Map<Vector2i, Vector2i> segments = new HashMap<>();
        while(!borders.isEmpty()){
            Vector2i start = borders.iterator().next();
            Vector2i prev;
            while(borders.contains(prev = start.sub(forwardOffset))) start = prev;

            Vector2i end = start;
            Vector2i next;
            borders.remove(start);
            while(borders.contains(next = end.add(forwardOffset))){
                end = next;
                borders.remove(next);
            }
            segments.put(start, end);
        }
        return segments;
    }

    public static enum Direction{
        NORTH(1, 3, Vector2i.from(1, -1)),
        EAST(2, 0, Vector2i.from(1, 1)),
        SOUTH(3, 1, Vector2i.from(-1, 1)),
        WEST(0, 2, Vector2i.from(-1, -1));

        private final int primary;
        private final int secondary;
        private final Vector2i secondaryOffset;
        Direction(int primary, int secondary, Vector2i secondaryOffset) {
            this.primary = primary;
            this.secondary = secondary;
            this.secondaryOffset = secondaryOffset;
        }

        public Direction getPrimary(){
            return Direction.values()[primary];
        }

        public Direction getSecondary(){
            return Direction.values()[secondary];
        }

        public Vector2i getSecondaryOffset() {
            return secondaryOffset;
        }
    }
}