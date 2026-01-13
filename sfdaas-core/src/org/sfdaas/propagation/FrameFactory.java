package org.sfdaas.propagation;

import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.errors.OrekitException;

/**
 * Factory class for creating OreKit Frame objects based on FrameType.
 * Provides access to common reference frames for orbit propagation.
 *
 * @author SFDaaS
 */
public class FrameFactory {

    /**
     * Create an OreKit Frame based on the specified type.
     *
     * @param type The frame type
     * @return An OreKit Frame instance
     * @throws OrekitException if frame cannot be created
     */
    public static Frame createFrame(FrameType type) throws OrekitException {
        switch (type) {
            case EME2000:
                return FramesFactory.getEME2000();

            case GCRF:
                return FramesFactory.getGCRF();

            case ITRF:
                return FramesFactory.getITRF(org.orekit.utils.IERSConventions.IERS_2010, true);

            case TEME:
                return FramesFactory.getTEME();

            case MOD:
                return FramesFactory.getMOD(org.orekit.utils.IERSConventions.IERS_2010);

            case TOD:
                return FramesFactory.getTOD(org.orekit.utils.IERSConventions.IERS_2010, true);

            default:
                // Fallback to EME2000
                return FramesFactory.getEME2000();
        }
    }
}
