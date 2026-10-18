package com.badimps.personaltrainer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;

import java.io.File;

/** Guards the hosted Trinidad & Tobago package from being replaced with a blank vector map. */
public final class OfflineMapPackageTest {
    @Test public void trinidadPackageContainsStreetDataNearPortOfSpain() {
        File packageFile = new File("../docs/offline/trinidad-tobago.map");
        if (!packageFile.isFile()) packageFile = new File("docs/offline/trinidad-tobago.map");
        MapFile map = new MapFile(packageFile);
        try {
            BoundingBox bounds = map.boundingBox();
            assertTrue(bounds.contains(10.6918d, -61.2225d));
            byte zoom = 13;
            int tileX = (int) Math.floor(((-61.2225d + 180d) / 360d) * (1 << zoom));
            double latRad = Math.toRadians(10.6918d);
            double mercator = Math.log(Math.tan(Math.PI / 4d + latRad / 2d));
            int tileY = (int) Math.floor((1d - mercator / Math.PI) / 2d * (1 << zoom));
            MapReadResult data = map.readMapData(new Tile(tileX, tileY, zoom, 256));
            assertFalse("The hosted map has no drawable ways near Port of Spain", data.ways.isEmpty());
            boolean hasRoad = false;
            for (Way way : data.ways) for (org.mapsforge.core.model.Tag tag : way.tags) {
                if ("highway".equals(tag.key)) { hasRoad = true; break; }
            }
            assertTrue("The hosted map has no highway tags that the street theme can draw", hasRoad);
        } finally { map.close(); }
    }
}
