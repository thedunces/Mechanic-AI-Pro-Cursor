import { GoogleGenerativeAI, SchemaType } from "@google/generative-ai";
import { HttpsError, onCall } from "firebase-functions/v2/https";

interface DiagnosticRequest {
  vehicle: {
    make: string;
    model: string;
    year: number;
    vin?: string;
    engine?: string;
    trim?: string;
  };
  inputs: {
    obdCodes?: string[];
    liveData?: { name: string; value: string; unit: string }[];
    symptoms?: string;
    notes?: string;
  };
}

interface DiagnosticResponse {
  explanation: string;
  likelyCauses: string[];
  recommendedFixes: string[];
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | "UNKNOWN";
  partsNeeded: string[];
  safetyNotes: string[];
  whenToSeeMechanic: string;
}

/**
 * Cloud Function that calls the Gemini API to produce a structured diagnosis.
 * The API key is read from the GOOGLE_API_KEY environment variable (set in functions/.env).
 * In production, migrate to Firebase AI Logic / Vertex AI and enforce App Check.
 */
export const diagnose = onCall(
  {
    maxInstances: 10,
    memory: "256MiB",
  },
  async (request): Promise<DiagnosticResponse> => {
    // Optional: enforce App Check in production.
    // if (request.app == null) {
    //   throw new functions.HttpsError("unauthenticated", "App Check required");
    // }

    const data = request.data as DiagnosticRequest;
    const vehicle = data.vehicle;
    const inputs = data.inputs ?? {};

    if (!vehicle || !vehicle.make || !vehicle.model || !vehicle.year) {
      throw new HttpsError("invalid-argument", "Vehicle make, model, and year are required");
    }

    const apiKey = process.env.GOOGLE_API_KEY;
    if (!apiKey) {
      throw new HttpsError("internal", "AI backend is not configured");
    }

    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({
      model: "gemini-1.5-flash",
      generationConfig: {
        responseMimeType: "application/json",
        responseSchema: {
          type: SchemaType.OBJECT,
          properties: {
            explanation: { type: SchemaType.STRING },
            likelyCauses: {
              type: SchemaType.ARRAY,
              items: { type: SchemaType.STRING },
            },
            recommendedFixes: {
              type: SchemaType.ARRAY,
              items: { type: SchemaType.STRING },
            },
            severity: {
              type: SchemaType.STRING,
              enum: ["LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN"],
            },
            partsNeeded: {
              type: SchemaType.ARRAY,
              items: { type: SchemaType.STRING },
            },
            safetyNotes: {
              type: SchemaType.ARRAY,
              items: { type: SchemaType.STRING },
            },
            whenToSeeMechanic: { type: SchemaType.STRING },
          },
          required: [
            "explanation",
            "likelyCauses",
            "recommendedFixes",
            "severity",
            "partsNeeded",
            "safetyNotes",
            "whenToSeeMechanic",
          ],
        },
      },
    });

    const codes = inputs.obdCodes?.join(", ") ?? "none provided";
    const liveData = inputs.liveData
      ?.map((p) => `${p.name}: ${p.value} ${p.unit}`)
      .join("; ") ?? "none provided";

    const prompt = `
You are an experienced automotive diagnostic assistant helping a DIY mechanic.
Use the vehicle information, OBD-II codes, live data parameters, symptoms, and notes below.
Always include safety warnings if the issue may affect brakes, steering, airbags, fuel, high voltage, or safe driving.
Be concise but thorough. If you are uncertain, say so and recommend seeing a professional.

Vehicle: ${vehicle.year} ${vehicle.make} ${vehicle.model} ${vehicle.trim ?? ""} ${vehicle.engine ?? ""}
VIN: ${vehicle.vin ?? "not provided"}

OBD-II codes: ${codes}
Live data parameters: ${liveData}
Symptoms: ${inputs.symptoms ?? "not provided"}
Additional notes: ${inputs.notes ?? "not provided"}

Provide a structured JSON diagnosis with the following fields:
- explanation: a short summary of what the problem likely is
- likelyCauses: an array of the most likely causes, ordered from most to least likely
- recommendedFixes: an array of step-by-step repair or diagnostic actions a DIYer can perform
- severity: one of LOW, MEDIUM, HIGH, CRITICAL, UNKNOWN
- partsNeeded: an array of parts or tools that may be needed
- safetyNotes: an array of safety warnings
- whenToSeeMechanic: guidance on when to stop DIY work and consult a professional
`.trim();

    try {
      const result = await model.generateContent(prompt);
      const text = result.response.text();
      return JSON.parse(text) as DiagnosticResponse;
    } catch (error) {
      console.error("Diagnosis failed:", error);
      throw new HttpsError("internal", "Failed to generate diagnosis");
    }
  }
);
