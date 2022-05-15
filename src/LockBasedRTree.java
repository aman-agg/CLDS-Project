import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class TASlock{
    AtomicBoolean state = new AtomicBoolean(false);
    void lock(){
        while (state.getAndSet(true));
    }
    void unlock(){
        state.set(false);
    }
}
public class LockBasedRTree implements Runnable{
    static Node root;
    private Lock lock = new ReentrantLock();
    int counter  = 0;
    static final int threadPoolSize = 5;
    ConcurrentHashMap<Integer, Integer> map
            = new ConcurrentHashMap<>();

    // Create the set by newKeySet() method of ConcurrentHashMap
    Set<Integer> set = map.newKeySet();


    LockBasedRTree(ReentrantLock lock) {
        this.lock = lock;
    }

    public void add(Point newPoint) {
        if (this.contains(newPoint) == true) {
            System.out.println("Point present in tree");
            return;
        }

            lock.lock();
            System.out.println("Locking Add" + Thread.currentThread().getId());
            try {
                boolean additionSuccessfull = false;
                if (newPoint == null)
                    return;
                if (root == null) {
                    System.out.println("Adding root is null case");
                    Node newNode = new Node();
                    newNode.leftEntry = new Entry();
                    newNode.leftEntry.lowerBottom = new Point(newPoint);
                    root = newNode;
                    additionSuccessfull = true;
                    System.out.println("Added new point - " + newPoint.getX() + " " + newPoint.getY());
                    return;
                }
                Node curNode = root;
                boolean traversal = true;
                while (traversal) {
                    boolean emptyLeaf = false;
                    boolean fullLeaf = false;
                    boolean internal = false;
                    if (curNode == null) {
                        traversal = false;
                        continue;
                    }
                    if (curNode.leftEntry == null || curNode.rightEntry == null) {
                        emptyLeaf = true;
                    }
                    // In case of empty leaf, add a new point
                    if (emptyLeaf) {
                        if (curNode.leftEntry == null) {
                            curNode.leftEntry = new Entry();
                            curNode.leftEntry.lowerBottom = new Point(newPoint);
                        } else {
                            curNode.rightEntry = new Entry();
                            curNode.rightEntry.lowerBottom = new Point(newPoint);
                        }
                        additionSuccessfull = true;
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
                        if (minSide % 2 == 0)
                            curNode = curNode.leftChild;
                        else
                            curNode = curNode.rightChild;
                        continue;
                    }
                    if (fullLeaf) {
                        // add new point and existing nearest point in a new child node of current node
                        long leftDist = calculatePointDistance(newPoint, curNode.leftEntry.lowerBottom);
                        long righDist = calculatePointDistance(newPoint, curNode.rightEntry.lowerBottom);

                        // add existing left/right point and the new point in a left/right child of current node
                        Node newCombinedChild = new Node();
                        newCombinedChild.parent = curNode;

                        newCombinedChild.leftEntry = new Entry();
                        newCombinedChild.leftEntry.lowerBottom = new Point(leftDist < righDist ? curNode.leftEntry.lowerBottom : curNode.rightEntry.lowerBottom);

                        newCombinedChild.rightEntry = new Entry();
                        newCombinedChild.rightEntry.lowerBottom = new Point(newPoint);

                        if (leftDist < righDist)
                            curNode.leftChild = newCombinedChild;
                        else
                            curNode.rightChild = newCombinedChild;

                        // add existing right/left point in new right/left child of current node
                        Node newSingularChild = new Node();
                        newSingularChild.parent = curNode;

                        newSingularChild.leftEntry = new Entry();
                        newSingularChild.leftEntry.lowerBottom = new Point(leftDist < righDist ? curNode.rightEntry.lowerBottom : curNode.leftEntry.lowerBottom);

                        if (leftDist < righDist)
                            curNode.rightChild = newSingularChild;
                        else
                            curNode.leftChild = newSingularChild;

                        additionSuccessfull = true;
                        traversal = false;
                    }
                }
                if (additionSuccessfull) {
                    System.out.println("Added new point - " + newPoint.getX() + " " + newPoint.getY());
                    // update MBRs of all ancestors
                    updateMBR(curNode);
                }
            } finally {
                System.out.println("Unlocking Add" + Thread.currentThread().getId());
                lock.unlock();
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
        lock.lock();
        try {
            Queue<Node> q = new LinkedList<>();
            if(root == null){
                return false;
            }
            q.add(root);
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
            lock.unlock();
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
    public void scan() {
        lock.lock();
        try {
            Queue<Node> q = new LinkedList<>();
            if(root != null) {

                q.add(root);

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
            lock.unlock();
        }
    }

    public void printEntry(Entry e) {
        if (e == null) {
            System.out.print("null");
        } else {
            System.out.print("lowerbottom: " + e.lowerBottom.x + " " + e.lowerBottom.y+" ");
            if (e.upperTop == null) {
                System.out.println("uppertop null ");
            } else {
                System.out.println("uppertop: "+ e.upperTop.x +" "+e.upperTop.y+" ");
            }
        }
    }
    public void rangeSearch(Point p1, Point p2){
        System.out.println("Range Search Started");
        lock.lock();
        try {
            Entry range = new Entry();
            range.lowerBottom = p1;
            range.upperTop = p2;
            ArrayList<Point> rangePoints = new ArrayList<>();
            Queue<Node> q = new LinkedList<>();
            q.add(root);
//            int level = 0;
//            System.out.println("Level " + level);
            while (q.size() != 0) {
                Node curr = q.poll();
                if(curr == null)
                    continue;
                if (curr.leftEntry != null) {
                    if (curr.leftEntry.upperTop == null) {
                        if (checkPointInMBR(range, curr.leftEntry.lowerBottom)) {
                            rangePoints.add(curr.leftEntry.lowerBottom);
                        }
                    } else {
                        if (compareMBR(range, curr.leftEntry)) {
                            q.add(curr.leftChild);
                        }
                    }
                }
                if (curr.rightEntry != null) {
                    if (curr.rightEntry.upperTop == null) {
                        if (checkPointInMBR(range, curr.rightEntry.lowerBottom)) {
                            rangePoints.add(curr.rightEntry.lowerBottom);
                        }
                    } else {
                        if (compareMBR(range, curr.rightEntry)) {
                            q.add(curr.rightChild);
                        }
                    }
                }
            }
            System.out.println("Range Points: ");
            for (Point curr : rangePoints) {
                System.out.println(curr.x + " " + curr.y);
            }
            System.out.println();
        }
        finally{
            lock.unlock();
        }
    }
    private boolean compareMBR(Entry range, Entry rect) {
        if(range.upperTop.getY() < rect.lowerBottom.getY() || range.lowerBottom.getY() > rect.upperTop.getY()){
            return false;
        }
        if(range.upperTop.getX() < rect.lowerBottom.getX() || range.lowerBottom.getX() > rect.upperTop.getX()){
            return false;
        }
        return true;
    }

    public void delete(Point delPoint) {
        if (this.contains(delPoint) == false) {
            System.out.println("Point not present in tree");
            return;
        }
//        System.out.println("Breakpoint 1");
        lock.lock();
        try {

            Node curr = root;
            while (true) {
                if (curr.leftEntry != null && curr.leftEntry.isPoint() && curr.leftChild == null) {
                    break;
                }
                if (curr.rightEntry != null && curr.rightEntry.isPoint() && curr.rightChild == null) {
                    break;
                }
                if (checkPointInMBR(curr.leftEntry, delPoint)) {
                    curr = curr.leftChild;
                } else {
                    curr = curr.rightChild;
                }
            }

            boolean fullLeaf = false;
            boolean emptyLeaf = false;
            if (curr.leftEntry != null && curr.rightEntry != null) {
                fullLeaf = true;
            } else {
                emptyLeaf = true;
            }
            if (fullLeaf) {
                if (checkPointWithEntry(curr.leftEntry, delPoint)) {
                    curr.leftEntry = null;
                } else if (checkPointWithEntry(curr.rightEntry, delPoint)) {
                    curr.rightEntry = null;
                }
                Node parent = curr.parent;
                if (parent == null) {
                    //curr is the root node
                    root = curr;
                    return;
                } else if (parent == root) {
                    root = parent;
                    curr = parent;
                    updateMBR(curr);
                    return;
                } else {
                    //parent is the root node
                    curr = parent;
                    updateMBR(curr);
                    return;
                }
            }
            if (emptyLeaf) {
                curr.leftEntry = null;
                curr.rightEntry = null;
                if (curr.parent == null) {
                    //curr is root node
                    root = null;
                    return;
                }
                Node parent = curr.parent;
                if (parent.leftChild.equals(curr)) {
                    Node grandParent = parent.parent;
                    if (grandParent == null) {
                        //parent is root
                        parent = parent.rightChild;
                        parent.parent = null;
                        root = parent;
                    } else {
                        Node toBeReplaced = parent.leftChild.equals(curr) ? parent.rightChild : parent.leftChild;
                        if (grandParent.leftChild.equals(parent)) {
                            grandParent.leftChild = toBeReplaced;
                        } else {
                            grandParent.rightChild = toBeReplaced;
                        }
                        toBeReplaced.parent = grandParent;
                        curr = toBeReplaced;
                    }
                } else {
                    Node grandParent = parent.parent;
                    if (grandParent == null) {
                        //parent is root
                        parent = parent.leftChild;
                        parent.parent = null;
                        root = parent;
                    } else {
                        Node toBeReplaced = parent.leftChild.equals(curr) ? parent.rightChild : parent.leftChild;
                        if (grandParent.leftChild.equals(parent)) {
                            grandParent.leftChild = toBeReplaced;
                        } else {
                            grandParent.rightChild = toBeReplaced;
                        }
                        toBeReplaced.parent = grandParent;
                        curr = toBeReplaced;
                    }
                }
                curr = curr.parent;
                updateMBR(curr);
            }
        }
        finally {
            lock.unlock();
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

    public int printOperations(){
        Scanner sc = new Scanner(System.in);

        System.out.println("R Tree operations");
        System.out.println("1. Add (x y)");
        System.out.println("2. Delete (x y)");
        System.out.println("3. Print Tree");
        System.out.println("4. Contains");
        System.out.println("5. Range Search");
        System.out.println("6. Exit");
        int n = 0;
        while(n > 5 || n < 1){
            System.out.print("Choose operation number: ");
            n = sc.nextInt();
            System.out.println();
        }
        return n;
    }
    @Override
    public void run() {
        int[] operations = {1,1,1,2,1,2,1,5};
        int[][] inputs = {{1,1},{1,2},{3,3},{1,4},{2,4},{2,2},{1,4},{}};
//        int[] operations = {1,1,1,2,1,2,1};
//        int[][] inputs = {{1,1},{2,2},{3,3},{1,1},{2,4},{2,2},{1,4}};
//        int[] operations = {1,1,1,1,1,1,1};
//        int[][] inputs = {{1,1},{2,2},{3,3},{1,1},{2,4},{2,2},{1,4}};
        Scanner sc = new Scanner(System.in);
        Random rand = new Random();
        int opInd = rand.nextInt(operations.length);
        System.out.println("Operation index 1: "+opInd);

        if(counter > 8){
            System.out.println("Scan");
            this.scan();

//            System.out.println(Thread.currentThread().getId());
        }
        else if(set.contains(opInd)){
            System.out.println("Already in set");
        }
        else {

            System.out.println("Operation index 2: "+opInd);
            set.add(opInd);
            int operation = operations[opInd];
            if (operation == 1) {
                System.out.print("Addition: ");
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                Point temp = new Point(x,y);
                System.out.println("Stage 1 Addition: "+temp.toString());
                this.add(temp);
                this.scan();
                Point a = new Point(0,0);
                Point b = new Point(2, 2);
                this.rangeSearch(a,b);
                System.out.println("Thread ID: " + Thread.currentThread().getId());
                counter++;
            } else if (operation == 2) {
                System.out.print("Deletion: ");
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                Point temp = new Point(x,y);
                System.out.println("Stage 1 Deletion: "+temp.toString());
                this.delete(temp);
                System.out.println("Thread ID: " + Thread.currentThread().getId());
                counter++;
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
            }
             else if (operation == 5) {
                 Point a = new Point(0,0);
                 Point b = new Point(2, 2);
                this.rangeSearch(a,b);
                counter++;
            }
            else if (operation == 6) {
                System.out.println("Thread ID: " + Thread.currentThread().getId());
            } else {
                System.out.println("Thread ID: " + Thread.currentThread().getId());
                System.out.println("Incorrect input");
            }
        }
    }
}
