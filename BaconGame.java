import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Kevin Bacon game player with included library of graph methods
 *
 * @author Josh Pfefferkorn
 * CS10, Fall 2020
 */

public class BaconGame {

    // declare strings for relevant path names containing actor and movie information
    static final String IDtoActor = "inputs/actors.txt";
    static final String IDtoMovie = "inputs/movies.txt";
    static final String movieIDtoActorID ="inputs/movie-actors.txt";

    /**
     * Takes a file with IDs and their related names (either movies or actors) and
     * maps the movies or actors to their IDs
     */
    public static Map<String,String> fileNameToMap(String fileName) {
        BufferedReader input;
        // initialize map
        Map<String,String> map = new HashMap<>();

        // try creating a reader for the file
        try {
            input = new BufferedReader(new FileReader(fileName));
        }
        // catch if the file doesn't exist
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
            // return empty map
            return map;
        }
        try {
            String line;
            // read input line by line
            while ((line = input.readLine()) != null) {
                // split it into 2 pieces, one for ID and one for name
                String[] pieces = line.split("\\|");
                // add these pieces to the map
                map.put(pieces[0],pieces[1]);
            }
        }
        // if error while reading, catch it
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }
        // return completed map
        return map;
    }

    /**
     * Takes names of three files (one that contains actors and their IDs, one that contains movies and their IDs, and one that
     * contains movie IDs and actor IDs that appeared in them) and converges them into a map that maps movie names to all
     * the actor names that appear in them
     */
    public static Map<String,Set<String>> mapMoviesToActors(String IDtoActorPathName,String IDtoMoviePathName,String IDtoIDPathName) {
        // map actor IDs to actor names and movie IDs to movie names
        Map<String,String> IDtoActor = fileNameToMap(IDtoActorPathName);
        Map<String,String> IDtoMovie = fileNameToMap(IDtoMoviePathName);

        BufferedReader input;
        Map<String,Set<String>> moviesToActors = new HashMap<>();

        // try creating a reader for the file
        try {
            input = new BufferedReader(new FileReader(IDtoIDPathName));
        }
        // catch if the file doesn't exist
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
            // return empty map
            return moviesToActors;
        }
        try {
            String line;
            // read input line by line
            while ((line = input.readLine()) != null) {
                String[] pieces = line.split("\\|");
                // if the movie doesn't yet exist in the map
                if (!moviesToActors.containsKey(IDtoMovie.get(pieces[0]))) {
                    // add it and and an empty set for actors
                    moviesToActors.put(IDtoMovie.get(pieces[0]),new HashSet<>());
                }
                // add the actor to the set of actors for that movie
                moviesToActors.get(IDtoMovie.get(pieces[0])).add(IDtoActor.get(pieces[1]));
            }
        }
        // if error while reading, catch it
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }
        // return completed map
        return moviesToActors;
    }

    /**
     * Takes names of three files (one that contains actors and their IDs, one that contains movies and their IDs, and one that
     * contains movie IDs and actor IDs that appeared in them) and creates a graph with actors as vertices and sets of common
     * movies as edges
     */
    public static Graph<String,Set<String>> buildGraph(String IDtoActorPathName,String IDtoMoviePathName,String IDtoIDPathName) {
        // maps movies to actors
        Map<String,Set<String>> moviesToActors = mapMoviesToActors(IDtoActorPathName,IDtoMoviePathName,IDtoIDPathName);
        Graph<String,Set<String>> graph = new AdjacencyMapGraph<>();

        // iterate over movies
        for (String movie: moviesToActors.keySet()) {
            // iterate over actors
            for (String actor: moviesToActors.get(movie)) {
                // if the graph doesn't already have the actor as a vertex
                if (!graph.hasVertex(actor)); {
                    // add it
                    graph.insertVertex(actor);
                }
            }
        }

        // iterate over movies
        for (String movie: moviesToActors.keySet()) {
            // iterate over actors
            for (String actor1 : moviesToActors.get(movie)) {
                // for each actor, iterate over the other actors
                for (String actor2 : moviesToActors.get(movie)) {
                    // if the actors aren't the same
                    if (!actor2.equals(actor1)) {
                        // if there isn't already an edge
                        if (!graph.hasEdge(actor1,actor2)) {
                            // add an edge
                            graph.insertUndirected(actor1, actor2, new HashSet<>());
                        }
                        // add movie to set of common movies between these two actors
                        graph.getLabel(actor1,actor2).add(movie);
                    }
                }
            }
        }
        // return completed graph
        return graph;
    }

    /**
     * Takes a graph and a vertex from that graph and creates a shortest path tree with that vertex as the root in which
     * all children point toward that vertex
     */

    public static <V,E> Graph<V,E> bfs(Graph<V,E> g, V source) {
        Graph<V,E> pathTree = new AdjacencyMapGraph<>();
        Queue<V> queue = new LinkedList<V>();

        // check if source is in graph
        if (g.hasVertex(source)) {
            // if so, add it to the queue
            queue.add(source);
            // and the shortest path tree
            pathTree.insertVertex(source);
            // while the queue still has vertices to visit
            while (!queue.isEmpty()) {
                // remove the vertex at the front of the queue
                V vertex = queue.remove();
                // and check its neighbors
                for (V neighbor : g.outNeighbors(vertex)) {
                    // if the neighbor isn't already in the shortest path tree
                    if (!pathTree.hasVertex(neighbor)) {
                        // insert it
                        pathTree.insertVertex(neighbor);
                        // and insert an edge back to its parent
                        pathTree.insertDirected(neighbor,vertex, g.getLabel(vertex, neighbor));
                        // add the neighbor to the queue to be visited
                        queue.add(neighbor);
                    }
                }
            }
        }
        // return completed graph
        return pathTree;
    }

    /**
     * Takes a shortest path graph and a vertex in that graph and finds the shortest path back to the root as a list
     */
    public static <V,E> List<V> getPath(Graph<V,E> tree, V v) {
        List<V> shortestPath = new ArrayList<>();
        // if vertex isn't in tree
        if (!tree.hasVertex(v)) {
            // return empty list
            return shortestPath;
        }
        // add the starting point to the list
        shortestPath.add(v);
        // while we haven't yet reached the root
        while (tree.outDegree(v) != 0) {
            // for all neighbors (there should only be 1 if the shortest path tree was created properly)
            for (V neighbor: tree.outNeighbors(v)) {
                // add it to the list at the end
                shortestPath.add(neighbor);
                // update the current vertex to be its neighbor
                v = neighbor;
            }
        }
        // return the completed list
        return shortestPath;
    }

    /**
     * Takes a graph and subgraph and determines which nodes are missing from the subgraph
     */
    public static <V,E> Set<V> missingVertices(Graph<V,E> graph, Graph<V,E> subgraph) {
        Set<V> missing = new HashSet<>();
        // for each vertex in the graph
        for (V vertex : graph.vertices()) {
            // if that vertex isn't in the subgraph
            if (!subgraph.hasVertex(vertex)) {
                // add it to the set of missing vertices
                missing.add(vertex);
            }
        }
        // return completed set
        return missing;
    }

    /**
     * Takes a graph and a vertex, the root, and finds average distance from a vertex on the graph
     * back to the root
     */
    public static <V,E> double averageSeparation(Graph<V,E> tree, V root) {
        // find total vertices; don't include the root itself as a vertex
        int vertices = tree.numVertices()-1;
        // run helper method to find total separation from root
        int total = totalSeparation(1,tree,root);
        // check that vertices isn't 0 to avoid dividing by 0
        if (vertices != 0) {
            // return total separation by number of non-root vertices as a double
            return 1.0 * total / vertices;
        }
        return 0;
    }
    /**
     * Takes the starting level (0), a graph, and its root, and finds the total distance from every vertex on the graph
     * back to the root
     */
    public static <V,E> int totalSeparation(int level, Graph<V,E> tree, V root) {
        // total for each level starts at 0
        int total = 0;
        // for each neighbor
        for (V neighbor: tree.inNeighbors(root)) {
            // add the current level to total
            total += level;
            // and recursively run total separation, incrementing the level
            total += totalSeparation(level+1,tree,neighbor);
        }
        // return total separation as an int
        return total;
    }

    public static void main(String[] args) {
        // create comparator (used later in main method to sort maps of actors to various stats)
        Comparator<Map.Entry<String, Double>> comparator = new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                Double d1 = e1.getValue();
                Double d2 = e2.getValue();
                return d1.compareTo(d2);
            }
        };

        // build a master graph with all the actors and movies
        Graph g = buildGraph(IDtoActor,IDtoMovie,movieIDtoActorID);
        // start the center of the universe as Kevin Bacon (can be changed)
        String curCenter = "Kevin Bacon";

        // print a list of commands
        System.out.println("Commands:");
        System.out.println("\tc: enter a number to list top or bottom number centers of universe by average separation");
        System.out.println("\td: enter a number followed by second number to list actors sorted by degree between those numbers");
        System.out.println("\ti: list actors with infinite separation from the current center");
        System.out.println("\tp: enter name to find path from that actor to the current center of the universe");
        System.out.println("\ts: enter a number followed by second number to list actors sorted by separation from the current center, with separation between those numbers");
        System.out.println("\tu: enter a name to make that actor the new center of the universe");
        System.out.println("\tq: quit game");

        // initialize a scanner to read command-line input from the user
        Scanner in = new Scanner(System.in);
        // store the inital input
        String statement = in.nextLine();

        // as long as the user doesn't use the "q" command to quit
        while (!statement.equals("q")) {
            // if input isn't in the list of commands, reject and print an error message
            if (!statement.equals("c") && !statement.equals("d") && !statement.equals("i") && !statement.equals("p") && !statement.equals("s") && !statement.equals("q") && !statement.equals("u")) {
                // prompt user to enter a valid command
                System.out.println("Invalid command, try again");
                statement = in.nextLine();
            }
            // if input reads "c"
            if (statement.equals("c")) {
                // prompt user to enter a number
                System.out.println("Enter number (+ or -) to get top number best centers of the universe or bottom number worst centers of the universe");
                // store that input as a an int
                int number = Integer.parseInt(in.nextLine());
                // initialize a map from actors to their average separation and a list for map entries to sort
                HashMap<Object,Double> actorsAndSeparation = new HashMap();
                ArrayList actorsAndSeparationList = new ArrayList();
                // for each actor in the graph
                for (Object actor : g.vertices()) {
                    // find its average separation
                    double separation = averageSeparation(bfs(g,actor),actor);
                    // add actors and their average separation to map
                    actorsAndSeparation.put(actor,separation);
                }
                // store map entries in a list so that they can be sorted by separation
                for(Map.Entry<Object, Double> e: actorsAndSeparation.entrySet()) {
                    actorsAndSeparationList.add(e);
                }
                // sort them
                Collections.sort(actorsAndSeparationList,comparator);
                // if the input number was positive
                if (number > 0) {
                    System.out.println("The best " + number + " centers of the universe based on average separation are: ");
                    // print out the top <number> actors with smallest average separation
                    for (int k =0; k< number; k++) {
                        System.out.println(actorsAndSeparationList.get(k));
                    }
                }
                // if the input number was negative
                else if (number < 0) {
                    // make it positive for loop and printing purposes
                    number = -1*number;
                    System.out.println("The worst " + number + " centers of the universe based on average separation are: ");
                    // print out the bottom <number> actors with largest average separation
                    for (int k = actorsAndSeparationList.size()-1; k > actorsAndSeparationList.size()-number-1; k--) {
                        System.out.println(actorsAndSeparationList.get(k));
                    }
                }
                else System.out.println("Invalid number!");
            }
            // if input reads "d"
            else if (statement.equals("d")) {
                // prompt user to enter two numbers, storing them as ints
                System.out.println("Enter lower bound for degree");
                int low = Integer.parseInt(in.nextLine());
                System.out.println("Enter upper bound for degree");
                int high = Integer.parseInt(in.nextLine());
                while (low>high) {
                    System.out.println("Lower bound cannot be above upper bound, try again");
                    System.out.println("Enter lower bound for separation");
                    low = Integer.parseInt(in.nextLine());
                    System.out.println("Enter upper bound for separation");
                    high = Integer.parseInt(in.nextLine());
                }
                // initialize a map from actors to their degrees and a list for map entries to sort
                HashMap<Object,Double> actorsAndDegree = new HashMap();
                ArrayList actorsAndDegreeList = new ArrayList();
                // for each actor on the graph
                for (Object actor : g.vertices()) {
                    // store their degree as a double
                    double degree = g.outDegree(actor);
                    // if it is within the bounds
                    if (degree > low && degree < high) {
                        // add it to the map
                        actorsAndDegree.put(actor, degree);
                    }
                }
                // store map entries in a list so that they can be sorted by degree
                for(Map.Entry<Object, Double> e: actorsAndDegree.entrySet()) {
                    actorsAndDegreeList.add(e);
                }
                // sort them
                Collections.sort(actorsAndDegreeList,comparator);
                System.out.println("The actors with degrees between " + low + " and " + high + " sorted from high to low are:");
                // while there are still entries in the list
                while (!actorsAndDegreeList.isEmpty()) {
                    // print each one in order from highest degree to lowest
                    System.out.println(actorsAndDegreeList.remove(actorsAndDegreeList.size()-1));
                }
            }
            // if input reads "i"
            else if(statement.equals("i")) {
                // create a shortest path tree to the current center
                Graph bfs = bfs(g,curCenter);
                // pring the unreachable vertices
                System.out.println("These " + (missingVertices(g,bfs)).size() + " actors are unreachable by " + curCenter +": " +missingVertices(g,bfs));
            }
            // if input reads "p"
            else if (statement.equals("p")) {
                // prompt user to enter a name to find path to center
                System.out.println("Enter name to find path from actor to " + curCenter);
                // store that actor
                String person = in.nextLine();
                // if the actor is unreachable, prompt user to try again
                // create a shortest path tree to the current center
                Graph bfs = bfs(g,curCenter);
                // check if person is an actor
                while (!g.hasVertex(person)) {
                    // if not, prompt user to try again
                    System.out.println("Can't find actor " + person + ". Please try again");
                    person = in.nextLine();
                }
                // check if person is reachable
                while (!bfs.hasVertex(person)) {
                    // if not, prompt user to try again
                    System.out.println(person + " is unreachable. Please try again.");
                    person = in.nextLine();
                }
                // find the path to the center as a list
                List path = getPath(bfs,person);
                // find the number by subtracting 1 from the path length
                System.out.println(person + "'s number is " + (path.size()-1));
                // loop through the list
                for (int k = 0; k<path.size()-1; k++) {
                    // print out each relationship in the list
                    System.out.println(path.get(k) + " appeared in " + bfs.getLabel(path.get(k),path.get(k+1)) + " with " + path.get(k+1));
                }
            }
            // if input reads "s"
            else if (statement.equals("s")) {
                // prompt user to enter two numbers, storing them as ints
                System.out.println("Enter lower bound for separation");
                int low = Integer.parseInt(in.nextLine());
                System.out.println("Enter upper bound for separation");
                int high = Integer.parseInt(in.nextLine());
                // if lower bound is greater than upper bound, prompt user to try again
                while (low>high) {
                    System.out.println("Lower bound cannot be above upper bound, try again");
                    System.out.println("Enter lower bound for separation");
                    low = Integer.parseInt(in.nextLine());
                    System.out.println("Enter upper bound for separation");
                    high = Integer.parseInt(in.nextLine());
                }
                // initialize a map from actors to their separation from the center and a list for map entries to sort
                HashMap<Object,Double> actorsAndSeparation = new HashMap();
                ArrayList actorsAndSeparationList = new ArrayList();
                // create a shortest path tree to the current center
                Graph bfs = bfs(g,curCenter);
                // for each actor in the tree
                for (Object actor : bfs.vertices()) {
                    // excluding the center itself
                    if (!actor.equals(curCenter)) {
                        // find the separation from the center
                        double separationFromCenter = getPath(bfs,actor).size()-1;
                        // if it is within the bounds
                        if (separationFromCenter > low && separationFromCenter < high) {
                            // add the actor and their separation from the center to the map
                            actorsAndSeparation.put(actor, separationFromCenter);
                        }
                    }
                }
                // store map entries in a list so that they can be sorted by degree
                for(Map.Entry<Object, Double> e: actorsAndSeparation.entrySet()) {
                    actorsAndSeparationList.add(e);
                }
                // sort them
                Collections.sort(actorsAndSeparationList,comparator);
                System.out.println("The actors with separation from " + curCenter + " between " + low + " and " + high + " sorted from low to high are:");
                // while there are still entries in the list
                while (!actorsAndSeparationList.isEmpty()) {
                    // print each one in order from lowest separation to highest
                    System.out.println(actorsAndSeparationList.remove(0));
                }
            }
            // if input reads "u"
            else if (statement.equals("u")) {
                // prompt user to enter name for new center
                System.out.println("Enter name for new center of universe.");
                // update center to be that name
                curCenter = in.nextLine();
                // check if center is an actor
                while (!g.hasVertex(curCenter)) {
                    // if not, prompt user to try again
                    System.out.println("Can't find actor " + curCenter + ". Please try again");
                    curCenter = in.nextLine();
                }
                // make shortest path graph to that center
                Graph bfs = bfs(g,curCenter);
                // print the new center, the number of connected actors, and the average separation
                System.out.println(curCenter + " is the new center of the universe, connected to " + (bfs.numVertices()-1) + "/" + g.numVertices() + " actors with average separation " + averageSeparation(bfs,curCenter));
            }
            // update statement to be the next line
            statement = in.nextLine();
        }
        // when user enters "q", end game and stop running
        System.out.println("Game has ended");
        System.exit(0);
    }
}