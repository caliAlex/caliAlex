package a4;

import org.joml.Vector2f;

public class CurrentDragLocation
{
    public boolean isItDragging=  false;
    public Vector2f mostRecentPos = new Vector2f();
    public Vector2f changeInPos = new Vector2f();

    public void getChangeInPos(Vector2f newPos)
    {
        if(!isItDragging)
            changeInPos.set(0.0f, 0.0f);
        else
            newPos.sub(mostRecentPos, changeInPos);
        mostRecentPos.set(newPos);
    }

    public void dragBegin(int x, int y)
    {
        mostRecentPos.set(x, y);
        isItDragging = true;
    }

    public void dragEnd(){
        isItDragging = false;
    }
}
