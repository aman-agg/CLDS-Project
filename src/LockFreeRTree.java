import java.awt.*;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

public class LockFreeRTree implements Runnable{

    // FOR TESTING PURPOSES
    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    private static AtomicReference<Node> root = new AtomicReference<Node>();
    int counter  = 0;
    static final int threadPoolSize = 5;
    ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();

    // Create the set by newKeySet() method of ConcurrentHashMap
    Set<Integer> set = map.newKeySet();

    private static final AtomicReferenceFieldUpdater<Node,Node> leftChildUpdater = newUpdater(Node.class, Node.class, "leftChild");
    private static final AtomicReferenceFieldUpdater<Node,Node> rightChildUpdater = newUpdater(Node.class, Node.class, "rightChild");

    LockFreeRTree()
    {
        root.set(null);
    }
    public void add(Point newPoint) {
        if (this.contains(newPoint) == true) {
            System.out.println("Point present in tree");
            return;
        }

        System.out.println("Thread Id : "+Thread.currentThread().getId()+ " Point not present. Trying to add point "+newPoint.toString());
            try {
                boolean restartAddition = true;
                while (restartAddition) {
                    boolean additionSuccessfull = false;
                    if (newPoint == null)
                    {
                        return;
                    }
                    if (root.get() == null) {
                        System.out.println("Case of NULL root (Addition)");
                        Node newNode = new Node();
                        newNode.leftEntry = new Entry();
                        newNode.leftEntry.lowerBottom = new Point(newPoint);
                        if(root.compareAndSet(null, newNode)) {
                            additionSuccessfull = true;
                            restartAddition = false;
                            System.out.println("Added new point - " + newPoint.getX() + " " + newPoint.getY());
                        }
                        continue;
                    }
                    Node parent = null;
                    boolean parentChildLinkLeft=true;
                    Node curNode = root.get();
                    boolean traversal = true;
                    while (traversal) {
                        boolean emptyLeaf = false;
                        boolean fullLeaf = false;
                        boolean internal = false;
                        if (curNode == null) {
                            traversal = false;
                            break;
                        }

                        // Case of empty leaf, add a new point
                        if (curNode.leftEntry == null || curNode.rightEntry == null) {
                            Node newNode = curNode.getCopy();
                            if (newNode.leftEntry == null) {
                                newNode.leftEntry = new Entry();
                                newNode.leftEntry.lowerBottom = new Point(newPoint);
                            } else {
                                newNode.rightEntry = new Entry();
                                newNode.rightEntry.lowerBottom = new Point(newPoint);
                            }
                            if(parent == null && root.compareAndSet(curNode, newNode))
                            {
                                // CASE of only one leaf node that is root
                                additionSuccessfull = true;
                                restartAddition = false;
                                traversal= false;
                            }
                            else if(parent!=null && parentChildLinkLeft
                                    ? leftChildUpdater.compareAndSet(parent, curNode, newNode)
                                    : rightChildUpdater.compareAndSet(parent, curNode, newNode))
                            {
                                additionSuccessfull = true;
                                restartAddition =false;
                                traversal = false;
                            }
                            break;
                        }
                        // check for a internal node or a full leaf
                        if (curNode.leftEntry != null && curNode.rightEntry != null) {
                            // distinguish between internal node and a full leaf node using upperTop coordinates or using child links
                            if (curNode.leftEntry.upperTop == null && curNode.rightEntry.upperTop == null)
                                fullLeaf = true;
                            else
                                internal = true;
                        }
                        if (internal) {
                            int minSide = findMinMBRWhileAdd(newPoint, curNode);
//                    System.out.println("Traversing while addition to side (0 or 1) : " + minSide);
                            if (minSide % 2 == 0) {
                                parent = curNode;
                                curNode = curNode.leftChild;
                                parentChildLinkLeft = true;
                            }
                            else {
                                parent = curNode;
                                curNode = curNode.rightChild;
                                parentChildLinkLeft = false;
                            }
                            continue;
                        }
                        if (fullLeaf) {
                            // add new point and existing nearest point in a new child node of current node
                            long leftDist = calculatePointDistance(newPoint, curNode.leftEntry.lowerBottom);
                            long righDist = calculatePointDistance(newPoint, curNode.rightEntry.lowerBottom);

                            Node newCurNode = curNode.getCopy();
                            // add existing left/right point and the new point in a left/right child of current node

                            Node newCombinedChild = new Node();
                            newCombinedChild.parent = newCurNode;

                            newCombinedChild.leftEntry = new Entry();
                            newCombinedChild.leftEntry.lowerBottom = new Point(leftDist < righDist ? newCurNode.leftEntry.lowerBottom : newCurNode.rightEntry.lowerBottom);

                            newCombinedChild.rightEntry = new Entry();
                            newCombinedChild.rightEntry.lowerBottom = new Point(newPoint);

                            if (leftDist < righDist) {
                                newCurNode.leftChild = newCombinedChild;
                                newCurNode.leftEntry = calculateMBR(newCombinedChild.leftEntry, newCombinedChild.rightEntry);
                            }
                            else {
                                newCurNode.rightChild = newCombinedChild;
                                newCurNode.rightEntry = calculateMBR(newCombinedChild.leftEntry, newCombinedChild.rightEntry);
                            }
                            // add existing right/left point in new right/left child of current node
                            Node newSingularChild = new Node();
                            newSingularChild.parent = newCurNode;

                            newSingularChild.leftEntry = new Entry();
                            newSingularChild.leftEntry.lowerBottom = new Point(leftDist < righDist ? newCurNode.rightEntry.lowerBottom : newCurNode.leftEntry.lowerBottom);

                            if (leftDist < righDist) {
                                newCurNode.rightChild = newSingularChild;
                                newCurNode.rightEntry = calculateMBR(newSingularChild.leftEntry, newSingularChild.rightEntry);
                            }
                            else {
                                newCurNode.leftChild = newSingularChild;
                                newCurNode.leftEntry = calculateMBR(newSingularChild.leftEntry, newSingularChild.rightEntry);

                            }
                            // CAS
                            if(parent == null && root.compareAndSet(curNode, newCurNode))
                            {
                                // CASE of only one leaf node that is root
                                additionSuccessfull = true;
                                restartAddition = false;
                                traversal= false;
                            }
                            else if(parent!=null && parentChildLinkLeft
                                    ? leftChildUpdater.compareAndSet(parent, curNode, newCurNode)
                                    : rightChildUpdater.compareAndSet(parent, curNode, newCurNode))
                            {
                                System.out.println("Thread ID : " + Thread.currentThread().getId()+" Addition Successful - "+newPoint.toString());
                                additionSuccessfull = true;
                                restartAddition =false;
                                traversal = false;
                            }
                        }
                    }
                    if (additionSuccessfull) {
                        // update MBRs of all ancestors
                        updateMBR(curNode);
                    }
                }
            } finally {
                System.out.println("Addition Execution completed by " + Thread.currentThread().getId());
            }

    }
    public void updateMBR(Node curNode)
    {
        // update MBRs of all nodes and its ancestors starting from curNode

            if (curNode == null)
                return;
            boolean isLeftChildEmptyLeaf = false;
            boolean isRightChildEmptyLeaf = false;
            if (curNode.leftChild != null) {
                Entry expectedMBR = calculateMBR(curNode.leftChild.leftEntry, curNode.leftChild.rightEntry);
                if (curNode.leftChild.leftEntry == null || curNode.leftChild.rightEntry == null) {
                    isLeftChildEmptyLeaf = true;
                }
                if (!curNode.leftEntry.equals(expectedMBR))
                    curNode.leftEntry = expectedMBR;
            }
            if (curNode.rightChild != null) {
                Entry expectedMBR = calculateMBR(curNode.rightChild.leftEntry, curNode.rightChild.rightEntry);
                if (curNode.rightChild.leftEntry == null || curNode.rightChild.rightEntry == null) {
                    isRightChildEmptyLeaf = true;
                }
                if (!curNode.rightEntry.equals(expectedMBR))
                    curNode.rightEntry = expectedMBR;
            }
            if (isLeftChildEmptyLeaf && isRightChildEmptyLeaf) {
                curNode.leftEntry = curNode.leftChild.leftEntry == null ? curNode.leftChild.rightEntry : curNode.leftChild.leftEntry;
                curNode.rightEntry = curNode.rightChild.leftEntry == null ? curNode.rightChild.rightEntry : curNode.rightChild.leftEntry;
                curNode.leftChild = null;
                curNode.rightChild = null;
            }
            updateMBR(curNode.parent);


    }
    public boolean contains(Point p){
        try {
            Queue<Node> q = new LinkedList<>();
            if(root.get() == null){
                return false;
            }
            q.add(root.get());
            q.add(null);
            int level = 0;
            while (q.size() != 0) {
                Node curr = q.poll();
                if (curr == null) {
                    if (q.size() == 0) {
                        break;
                    }
                    level++;
                    q.add(null);
                    continue;
                }
                if (curr.leftEntry != null && curr.leftEntry.isPoint()) {
                    if (p.equals(curr.leftEntry.lowerBottom)) {
                        return true;
                    }
                }
                if (curr.rightEntry != null && curr.rightEntry.isPoint()) {
                    if (p.equals(curr.rightEntry.lowerBottom)) {
                        return true;
                    }
                }
                if (curr.leftChild != null) {
                    q.add(curr.leftChild);
                }
                if (curr.rightChild != null) {
                    q.add(curr.rightChild);
                }
            }
            return false;
        }
        finally {

        }
    }
    public Entry calculateMBR(Entry leftEntry, Entry rightEntry)
    {

            Entry ans = new Entry();
            ans.lowerBottom = new Point();
            if (leftEntry != null) {
                ans.lowerBottom.x = (int) leftEntry.lowerBottom.getX();
                ans.lowerBottom.y = (int) leftEntry.lowerBottom.getY();
            }
            if (rightEntry != null) {
                ans.lowerBottom.x = (int) Math.min(rightEntry.lowerBottom.getX(), ans.lowerBottom.x);
                ans.lowerBottom.y = (int) Math.min(rightEntry.lowerBottom.getY(), ans.lowerBottom.y);
            }

            ans.upperTop = new Point();
            if (leftEntry != null) {
                ans.upperTop.x = (int) (leftEntry.upperTop != null ? leftEntry.upperTop.getX() : leftEntry.lowerBottom.getX());
                ans.upperTop.y = (int) (leftEntry.upperTop != null ? leftEntry.upperTop.getY() : leftEntry.lowerBottom.getY());
            }
            if (rightEntry != null) {
                ans.upperTop.x = (int) Math.max(ans.upperTop.getX(), rightEntry.upperTop != null ? rightEntry.upperTop.getX() : rightEntry.lowerBottom.getX());
                ans.upperTop.y = (int) Math.max(ans.upperTop.getY(), rightEntry.upperTop != null ? rightEntry.upperTop.getY() : rightEntry.lowerBottom.getY());
            }

            return ans;

    }
    public int findMinMBRWhileAdd(Point newPoint, Node curNode)
    {
        // return 0 for left child and 1 for right child

        // check if new point lies in left MBR

            if (curNode.leftEntry != null) {
                if (pointInRectOrNot(newPoint, curNode.leftEntry.lowerBottom, curNode.leftEntry.upperTop))
                    return 0;
            }
            if (curNode.rightEntry != null) {
                if (pointInRectOrNot(newPoint, curNode.rightEntry.lowerBottom, curNode.rightEntry.upperTop))
                    return 1;
            }

            long leftMBRArea = calculateNewRectArea(newPoint, curNode.leftEntry.lowerBottom, curNode.leftEntry.upperTop);
            long rightMBRArea = calculateNewRectArea(newPoint, curNode.rightEntry.lowerBottom, curNode.rightEntry.upperTop);

        return leftMBRArea<=rightMBRArea ? 0 : 1; //

    }
    public long calculateNewRectArea(Point newPoint, Point rectLowerBottom, Point rectUpperTop)
    {
        double minX = Math.min(newPoint.getX(), rectLowerBottom.getX());
        double maxX = Math.max(newPoint.getX(), rectUpperTop.getX());
        double minY = Math.min(newPoint.getY(), rectLowerBottom.getY());
        double maxY = Math.max(newPoint.getY(), rectUpperTop.getY());
        return (long)((maxX-minX)*(maxY-minY));
    }
    public boolean pointInRectOrNot(Point point, Point rectLowerBottom, Point rectUpperTop)
    {
        if( point.getX()>=rectLowerBottom.getX() &&
                point.getX()<=rectUpperTop.getX() &&
                point.getY()>=rectLowerBottom.getY() &&
                point.getY()<=rectUpperTop.getY()
        )
            return true;
        return false;
    }
    public long calculatePointDistance(Point a, Point b)
    {
        return (long)Math.sqrt(Math.pow(a.x-b.x,2)+Math.pow(a.y-b.y,2));
    }
    public static void scan() {
        try {
            if(root.get() == null){
                System.out.println("Empty tree");
            }
            Queue<Node> q = new LinkedList<>();
            if(root.get() != null) {

                q.add(root.get());

                q.add(null);
                int level = 0;
                System.out.println("Level " + level);
                while (q.size() != 0) {
                    Node curr = q.poll();
                    if (curr == null) {
                        if (q.size() == 0) {
                            break;
                        }
                        level++;
                        System.out.println("Level " + level);
                        q.add(null);
                        continue;
                    }
                    System.out.println("Node: ");
                    System.out.print("Left Entry: ");
                    printEntry(curr.leftEntry);
                    System.out.print("Right Entry: ");
                    printEntry(curr.rightEntry);
                    if (curr.leftChild != null) {
                        q.add(curr.leftChild);
                    }
                    if (curr.rightChild != null) {
                        q.add(curr.rightChild);
                    }
                }
                System.out.println();
            }
            else{
                System.out.println("Empty tree");
            }
        }
        finally {

        }
    }

    public static void printEntry(Entry e) {
        if (e == null) {
            System.out.println("null");
        } else {
            System.out.print("lowerbottom: " + e.lowerBottom.x + " " + e.lowerBottom.y+" ");
            if (e.upperTop == null) {
                System.out.println("uppertop null ");
            } else {
                System.out.println("uppertop: "+ e.upperTop.x +" "+e.upperTop.y+" ");
            }
        }
    }

    public void delete(Point delPoint) {

        if (this.contains(delPoint) == false) {
            System.out.println("Thread ID : "+ Thread.currentThread().getId()+ " Deletion: Point not present in tree");
            return;
        }
        System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Point present. Trying to del point " + delPoint.toString());

        try {
            boolean restartDeletion = true;
            while (restartDeletion) {
                Node curr = root.get();
                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion restarted " + delPoint.toString());
                Node forCas = root.get();
                if(delPoint == null){
                    return;
                }
                Node newNode = null;
                Node parent = null;
                Boolean isParentChildLinkLeft = null;  //null if current node is root
                while (true) {
                    if (curr.leftEntry != null && curr.leftEntry.isPoint() && curr.leftChild == null) {
                        break;
                    }
                    if (curr.rightEntry != null && curr.rightEntry.isPoint() && curr.rightChild == null) {
                        break;
                    }
                    if (checkPointInMBR(curr.leftEntry, delPoint)) {
                        parent = curr;
                        curr = curr.leftChild;
                        isParentChildLinkLeft = true;
                    } else {
                        parent = curr;
                        isParentChildLinkLeft = false;
                        curr = curr.rightChild;
                    }
                }
                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Traversal completed ");
//                printEntry(curr.leftEntry);
//                printEntry(curr.rightEntry);
                boolean fullLeaf = false;
                boolean emptyLeaf = false;


//                if(parent != null) {
//                    isParentChildLinkLeft = parent.leftChild.equals(curr);
//                }
//                else{
//                    System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: root empty leaf case");
//                    isParentChildLinkLeft = forCas.leftEntry.lowerBottom.equals(delPoint);
//                }
                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Found delete entry in leaf "+isParentChildLinkLeft);
                //This if is to create the newNode for replacement
                if (curr.leftEntry != null && curr.rightEntry != null) {
                    //full leaf case
                    System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Full leaf case ");
                    fullLeaf = true;
                    newNode = curr.getCopy();
                    if(curr.leftEntry.lowerBottom.equals(delPoint)){
                        newNode.leftEntry = null;
                    }
                    else if(curr.rightEntry.lowerBottom.equals(delPoint)){
                        newNode.rightEntry = null;
                    }
                    else{
                        break;
                    }
                } else {
                    //Empty Leaf case
                    System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: empty leaf case ");
                    emptyLeaf = true;
                    newNode = null;
                }
                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Point present. Found leaf case "+fullLeaf);
                if(parent == null && root.compareAndSet(forCas,newNode)){
                    //current node is root node
                    System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Point is in root. Successfully deleted ");
                    restartDeletion = false;
                    return;
                }
                else if(parent == null){
                    continue;
                }
                if (fullLeaf) {
                    System.out.println("Thread ID : "+ Thread.currentThread().getId()+ " Deletion: Full leaf case");
                    if(parent != null && isParentChildLinkLeft
                            ? leftChildUpdater.compareAndSet(parent,curr,newNode)
                            : rightChildUpdater.compareAndSet(parent,curr,newNode)){
                        newNode.parent = parent;
                        restartDeletion = false;
                    }
                    if(!restartDeletion)
                        updateMBR(newNode);
                }
                if (emptyLeaf) {
                    System.out.println("Thread ID : "+ Thread.currentThread().getId()+ " Deletion: Empty leaf case");
                    if(parent != null && isParentChildLinkLeft
                            ? leftChildUpdater.compareAndSet(parent,curr,newNode)
                            : rightChildUpdater.compareAndSet(parent,curr,newNode)){
                        restartDeletion = false;
                    }
                    if(!restartDeletion){
                        updateMBR(parent);
                    }
                }
            }
        }
        catch (Exception e){
            System.out.println(e);
        }
        finally{
                System.out.println("Deletion Execution completed by " + Thread.currentThread().getId());
            }

        }



    public boolean checkPointWithEntry(Entry rect, Point p) {

        if (rect == null) {
            return false;
        }
        if (rect.lowerBottom.x == p.x && rect.lowerBottom.y == p.y) {
            return true;
        }
        return false;
    }

    public boolean checkPointInMBR(Entry rect, Point p) {
        if (rect == null) {
            return false;
        }
        if (p.x < rect.lowerBottom.x || p.x > rect.upperTop.x) {
            return false;
        }
        if (p.y < rect.lowerBottom.y || p.y > rect.upperTop.y) {
            return false;
        }
        return true;
    }

    @Override
    public void run() {

//        int[] operations = {1,1,1,2,1,2,1,2};
//        int[][] inputs = {{1,1},{2,2},{3,3},{2,2},{4,4},{2,2},{5,5},{3,3}};
        int[] operations = {1,1,1,1,1,1,2,2,2,2,2}; // for full leaf non root deletion
        int[][] inputs = {{1,1},{3,3},{1,2},{2,3},{3,1},{2,1},{3,3},{1,2},{2,3},{3,1},{2,1}};
//        int[] operations = {1,1,1,2,2,2,1};
//        int[][] inputs = {{1,1},{2,2},{1,1},{2,2},{1,1},{2,2},{1,4}};
//        int[] operations = {1,1,1,1,1,2,1};
//        int[][] inputs = {{1,1},{2,2},{3,3},{1,1},{2,4},{2,2},{1,4}};
//        int[] operations = {1,1,2,2,2,2,1};
//        int[][] inputs = {{1,1},{2,2},{2,2},{2,2},{1,1},{2,2},{1,4}};
        Scanner sc = new Scanner(System.in);
        int prevAtomicValue = atomicInteger.get();
        int newAtomicValue = prevAtomicValue+1;
        while(true)
        {
            if(atomicInteger.compareAndSet(prevAtomicValue, newAtomicValue))
                break;
            prevAtomicValue=atomicInteger.get();
            newAtomicValue=prevAtomicValue+1;
        }
        int opInd = newAtomicValue;
        System.out.println("Operation Index chosen by Thread "+Thread.currentThread().getId()+ " : " + opInd);
//        while(threadSafeUniqueNumbers.contains(opInd) == false){
//            opInd = rand.nextInt(operations.length);
//        }

        if(set.contains(opInd)){
            System.out.println("Thread ID : "+ Thread.currentThread().getId()+ " Already in set");
        }
        else {
            set.add(opInd);
            int operation = operations[opInd];
            if (operation == 1) {
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                Point temp = new Point(x,y);
                System.out.println("Thread ID : "+ Thread.currentThread().getId()+ " Stage 1 Addition: "+temp.toString());
                this.add(temp);
                System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Addition");
                //this.scan();
                int prev = atomicInteger.get();
                while(!atomicInteger.compareAndSet(prev, prev+1)){
                    prev = atomicInteger.get();
                }
                System.out.println("Thread ID : "+Thread.currentThread().getId()+" Atomic counter "+atomicInteger);
//                if(atomicInteger.get()==8) {
//                    while (true) {
//                        prevAtomicValue = atomicInteger.get();
//                        newAtomicValue = prevAtomicValue + 1;
//                        if (atomicInteger.compareAndSet(prevAtomicValue, newAtomicValue)) {
////                            this.delete(new Point(2,2));
////                            this.scan();
////                            this.delete(new Point(3,3));
//                            this.scan();
//                            break;
//                        }
//                    }
//                }
                while (true) {
                    if(atomicInteger.get()==20) {
                        prevAtomicValue = atomicInteger.get();
                        newAtomicValue = prevAtomicValue + 1;
                        if (atomicInteger.compareAndSet(prevAtomicValue, newAtomicValue)) {
//                            this.scan();
                            System.out.println("Addition completed");
                            this.delete(temp);
                            System.out.println("Scan after delete");
                            this.scan();
                            System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Scan");
                            break;
                        }
                    }
                }
                System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Scan");

            } else if (operation == 2) {
                System.out.print("Deletion: ");
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                Point temp = new Point(x,y);
                System.out.println("Thread ID : "+ Thread.currentThread().getId()+ " Stage 1 Deletion: "+temp.toString());
                //this.delete(temp);
                System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Deletion");
//                this.scan();
                int prev = atomicInteger.get();
                while(!atomicInteger.compareAndSet(prev, prev+1)){
                    prev = atomicInteger.get();
                }
                System.out.println("Thread ID : "+Thread.currentThread().getId()+" Atomic counter "+atomicInteger);
                while (true) {
                    if(atomicInteger.get()==21) {
                        prevAtomicValue = atomicInteger.get();
                        newAtomicValue = prevAtomicValue;
                        if (atomicInteger.compareAndSet(prevAtomicValue, newAtomicValue)) {
//                            this.scan();
                            this.delete(temp);
                            System.out.println("Scan after delete");
                            this.scan();
                            System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Scan");
                            break;
                        }
                    }
                }

            } else if (operation == 3) {
                System.out.println("Scan");
                this.scan();
                System.out.println(Thread.currentThread().getId());
            } else if (operation == 4) {
                System.out.print("Contains: ");
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                System.out.println();
                System.out.println(this.contains(new Point(x,y)));
                System.out.println(Thread.currentThread().getId());
            } else if (operation == 5) {
                System.out.println("Thread ID: " + Thread.currentThread().getId());
            } else {
                System.out.println("Thread ID: " + Thread.currentThread().getId());
                System.out.println("Incorrect input");
            }
        }
    }
}
