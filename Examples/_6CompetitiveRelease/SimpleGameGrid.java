package Examples._6CompetitiveRelease;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.Gui.GridWindow;
import Framework.Gui.UIGrid;
import Framework.Gui.UIWindow;
import Framework.Rand;
import Framework.Util;

import static Framework.Util.CircleHood;
import static Framework.Util.MooreHood;
import static Framework.Util.RGB;

public class SimpleGameGrid extends AgentGrid2D<SimpleGameCell> {

    public SimpleGameGrid(int xDim, int yDim) {
        super(xDim, yDim, SimpleGameCell.class);

    }
    public void InitTumor(int radius) {

        Rand rn = new Rand();

        while (this.Pop()!=xDim*yDim) {
            int[] circleHood = CircleHood(true, radius);
            int mapHood = MapHood(circleHood, rn.Int(xDim), rn.Int(yDim));
            for(int i=0; i<mapHood; i++) {
                if (this.GetAgent(circleHood[i])==null) {
                    NewAgentSQ(circleHood[i]);
                } else {
                    continue;
                }
            }
        }
    }
    public void DrawModel(GridWindow win) {

//        for (int i = 0; i < length; i++) {
//            SimpleGameCell c = GetAgent(i);
//            win.SetPix(i, c == null ? RGB(0,0,0) : RGB(1,0,0));

        for(SimpleGameCell c : this) {
            win.SetPix(c.Xsq(), c.Ysq(), RGB(1,0,0));
        }
    }

    public static void main(String[] args) {

        SimpleGameGrid a = new SimpleGameGrid(100,100);
        GridWindow win = new GridWindow(100, 100, 1);
        a.InitTumor(10);
        a.DrawModel(win);
        System.out.println(a.Pop());
        //win.SetPix(10,10, Framework.Util.RGB(0,1,0));
        //int[] mooreHood = MooreHood(true);

    }
}

class SimpleGameCell extends AgentSQ2Dunstackable<SimpleGameGrid> {




}