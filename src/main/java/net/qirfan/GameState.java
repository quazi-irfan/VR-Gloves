package net.qirfan;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

public class GameState extends AbstractAppState{
    public Node allJointRoot = new Node("AllJointRoot");
    public Node topJointRoot = new Node("TopJointRoot");

    static public Geometry lowerJointGeo, midJointGeo, upperJointGeo, obstacleGeo;

    CollisionResults collisionResults = new CollisionResults();
    static boolean isHit = false;
    static float angle = 0;

    boolean isJointMovingForward = false, isJointMovingBackward = false;

    Trigger forwardTrigger = new KeyTrigger(KeyInput.KEY_1);
    Trigger backwardTrigger = new KeyTrigger(KeyInput.KEY_2);

    SimpleApplication application;
    Node gameStateNode;
    InputManager inputManager;
    AssetManager assetManager;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        // initialize application state properties
        this.application = (SimpleApplication)app;
        this.gameStateNode = this.application.getRootNode();
        this.inputManager = this.application.getInputManager();
        this.assetManager = this.application.getAssetManager();

        // setup ground grid
        Geometry grid = new Geometry("Grid", new Grid(10, 10, 1));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.White);
        grid.setMaterial(mat);
        gameStateNode.attachChild(grid);
        grid.setLocalTranslation(-4, 0, -4);

        // create a attach parts of the joint to the joint Node
        lowerJointGeo = makeCube("LowJoint", new Box(.5f,1f,.2f), Vector3f.ZERO, ColorRGBA.Blue);
        allJointRoot.attachChild(lowerJointGeo);

        midJointGeo = new Geometry("MidJoint", new Cylinder(16, 16, .2f, 1, true));
        Material midJointMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        midJointMat.setColor("Color", new ColorRGBA(.4f,.4f,.9f,1));
        midJointGeo.setMaterial(midJointMat);
        midJointGeo.setLocalRotation(new Quaternion(new float[]{0, 55, 0}));
        allJointRoot.attachChild(midJointGeo);

        upperJointGeo = makeCube("TopJoint", new Box(.5f,1f,.2f), Vector3f.ZERO, ColorRGBA.Blue);
        topJointRoot.attachChild(upperJointGeo);

        // reposition the joint sections relative to the joint node, and add attach the joint node to the root node
        lowerJointGeo.setLocalTranslation(0, 1f, 0);
        midJointGeo.setLocalTranslation(0, 2.25f, 0f);

        upperJointGeo.setLocalTranslation(0, 1.25f, 0);
        allJointRoot.attachChild(topJointRoot);
        topJointRoot.setLocalTranslation(0, 2.25f, 0);

        gameStateNode.attachChild(allJointRoot);
        allJointRoot.setLocalTranslation(0, 0, 2f);

        // setup obstacle geometry
        obstacleGeo = makeCube("Obstacle", new Box(1f,1f,1f), Vector3f.ZERO, ColorRGBA.Green);
        obstacleGeo.setLocalTranslation(0, 1.5f, 0);
        gameStateNode.attachChild(obstacleGeo);

        // setup I/O
//        inputManager.addMapping("forwardTrigger", forwardTrigger);
//        inputManager.addMapping("backwardTrigger", backwardTrigger);
//        inputManager.addListener(fingerMoveForwardListener, "forwardTrigger");
//        inputManager.addListener(fingerMoveBackwardListener, "backwardTrigger");

        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addListener(upListener, "up");
        inputManager.addListener(downListener, "down");
        inputManager.addListener(toggleListener, "toggle");
    }

    static boolean isBox = true;
    ActionListener toggleListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            Vector3f location = application.getRootNode().getChild("Obstacle").getLocalTranslation();
            if(isPressed){
                if(isBox) {
                    application.getRootNode().getChild("Obstacle").removeFromParent();
                    obstacleGeo = new Geometry("Obstacle", new Sphere(32, 32, 1.25f, true, false));
                    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                    mat.setColor("Color", ColorRGBA.Green);
                    obstacleGeo.setMaterial(mat);
                    obstacleGeo.setLocalTranslation(location);
                    application.getRootNode().attachChild(obstacleGeo);
                    isBox = false;
                }
                else
                {
                    application.getRootNode().getChild("Obstacle").removeFromParent();
                    obstacleGeo = makeCube("Obstacle", new Box(1f,1f,1f), Vector3f.ZERO, ColorRGBA.Green);
                    obstacleGeo.setLocalTranslation(location);
                    application.getRootNode().attachChild(obstacleGeo);
                    isBox = true;
                }
            }
        }
    };

//    ActionListener fingerMoveForwardListener = new ActionListener() {
//        public void onAction(String name, boolean isPressed, float tpf) {
//            isJointMovingForward = isPressed;
//        }
//    };
//
//    ActionListener fingerMoveBackwardListener = new ActionListener() {
//        public void onAction(String name, boolean isPressed, float tpf) {
//            isJointMovingBackward = isPressed;
//        }
//    };

    AnalogListener upListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            obstacleGeo.move(0, -tpf * 2, 0);
        }
    };

    AnalogListener downListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            obstacleGeo.move(0, tpf * 2, 0);
        }
    };

    @Override
    public void update(float tpf) {
        super.update(tpf);

//        if(isJointMovingForward){
//            topJointRoot.rotate(- 45 * FastMath.DEG_TO_RAD * tpf , 0 , 0);
//        }
//
//        if(isJointMovingBackward){
//            topJointRoot.rotate(45 * FastMath.DEG_TO_RAD * tpf , 0 , 0);
//        }

        // collision logic clears the collision result and checks for collision in every update loop
        collisionResults.clear();
        upperJointGeo.collideWith(obstacleGeo.getWorldBound(), collisionResults);
        if(collisionResults.getClosestCollision() != null &&
                collisionResults.getClosestCollision().getGeometry().getName().equals("TopJoint")){
            isHit = true;

//            if( isBox )
//            {
//                obstacleGeo.getMaterial().setColor("Color", ColorRGBA.Red);
//            }
        } else {
            isHit = false;
            obstacleGeo.getMaterial().setColor("Color", ColorRGBA.Green);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        // todo: remove all states
        gameStateNode.removeFromParent();
    }

    public Geometry makeCube(String name, Mesh mesh, Vector3f location, ColorRGBA colorId){
        Geometry geom = new Geometry(name, mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", colorId);
        geom.setMaterial(mat);
        geom.setLocalTranslation(location);
        return geom;
    }
}
