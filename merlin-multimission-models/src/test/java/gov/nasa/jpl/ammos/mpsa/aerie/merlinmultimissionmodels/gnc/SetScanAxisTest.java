package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.gnc.activities.SetScanAxis;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.List;
import java.util.Map;


public class SetScanAxisTest {

    @Test
    public void setScanAxis(){
        double x = 0.2;
        double y = 0.2;
        double z = 0.2;

        Instant simStart = SimulationInstant.fromQuantity(0, TimeUnit.MICROSECONDS);
        SetScanAxis setScanAxisActivity = new SetScanAxis(x, y, z);

        GNCStates gncStates = new GNCStates();

        SimulationEngine.simulate(simStart, List.of(Pair.of(simStart, setScanAxisActivity)), gncStates);

        Map<Instant, Vector3D> history = gncStates.getVectorState(GNCStates.scanAxisName).getHistory();

        Vector3D vector = gncStates.getVectorState(GNCStates.scanAxisName).get();
        Vector3D temp = new Vector3D(x, y, z);
        temp.normalize();
        System.out.println(temp.getX());

        assertTrue(vector.getX() == temp.getX());
        assertTrue(vector.getY() == temp.getY());
        assertTrue(vector.getZ() == temp.getZ());
    }

}
