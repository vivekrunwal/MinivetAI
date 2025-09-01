package com.vivek;

import ai.onnxruntime.*;

public class ModelInfo {
    public static void main(String[] args) {
        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            String modelPath = "models/all-MiniLM-L6-v2.onnx";
            OrtSession session = env.createSession(modelPath, new OrtSession.SessionOptions());
            
            System.out.println("=== Model Input Information ===");
            for (String inputName : session.getInputNames()) {
                System.out.println("Input: " + inputName);
                NodeInfo nodeInfo = session.getInputInfo().get(inputName);
                System.out.println("  Info: " + nodeInfo);
            }
            
            System.out.println("\n=== Model Output Information ===");
            for (String outputName : session.getOutputNames()) {
                System.out.println("Output: " + outputName);
                NodeInfo nodeInfo = session.getOutputInfo().get(outputName);
                System.out.println("  Info: " + nodeInfo);
            }
            
            session.close();
            env.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
