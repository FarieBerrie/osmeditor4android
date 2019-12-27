package de.blau.android;

import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import de.blau.android.dialogs.ElementInfo;
import de.blau.android.osm.OsmElement;

public class vmode extends Main {
    @Override
    public boolean onLitemodeCLick(final android.view.MenuItem item) {
        int itemId = item.getItemId() - clickedObjects.size();
        if ((itemId >= 0) && (clickedNodesAndWays != null) && (itemId < clickedNodesAndWays.size())) {
            final OsmElement element = clickedNodesAndWays.get(itemId);
            if (App.getLogic().isLocked()) {
                ElementInfo.showDialog(vmode.this, element);
            } else {
                Mode mode = App.getLogic().getMode();
                descheduleAutoLock();
                if (mode.elementsGeomEditiable()) {
                    if (doubleTap) {
                        doubleTap = false;
                        getEasyEditManager().startExtendedSelection(element);
                    } else {
                        getEasyEditManager().editElement(element);
                    }
                } else if (mode.elementsEditable()) {
                    performTagEdit(element, null, false, false);
                }
            }
        }
        return true;
    }

    @Override
    public boolean LmodeDoubleTap(View v, float x, float y) {
            boolean inEasyEditMode = logic.getMode().elementsGeomEditiable();
            boolean dataIsVisible = map.getDataLayer() != null && map.getDataLayer().isVisible();
            descheduleAutoLock();
            clickedNodesAndWays = dataIsVisible ? App.getLogic().getClickedNodesAndWays(x, y) : new ArrayList<>();
            switch (clickedNodesAndWays.size()) {
                case 0:
                    // no elements were touched
                    if (inEasyEditMode) {
                        // short cut to finishing multi-select
                        getEasyEditManager().nothingTouched(true);
                    }
                    break;
                case 1:
                    if (inEasyEditMode) {
                        getEasyEditManager().startExtendedSelection(clickedNodesAndWays.get(0));
                    }
                    break;
                default:
                    // multiple possible elements touched - show menu
                    if (inEasyEditMode) {
                        if (menuRequired()) {
                            Log.d(DEBUG_TAG, "displaying menu");
                            doubleTap = true; // ugly flag
                            v.showContextMenu();
                        } else {
                            getEasyEditManager().startExtendedSelection(clickedNodesAndWays.get(0));
                        }
                    }
                    break;
            }
        return true;
        }

}


