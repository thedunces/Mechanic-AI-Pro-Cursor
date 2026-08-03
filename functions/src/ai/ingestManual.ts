import { HttpsError, onCall } from "firebase-functions/v2/https";

/**
 * Future RAG ingestion function.
 * Safely ingests public-domain or licensed repair manuals into a vector store.
 *
 * TODO before production:
 *  - Verify copyright/terms of service for each source (archive.org, allcarmanuals.com, etc.)
 *  - Implement chunked text extraction and embedding generation
 *  - Store embeddings in a vector database (e.g., Firestore vector search, Pinecone, Weaviate)
 *  - Wire the diagnose function to retrieve relevant passages before calling Gemini
 */
export const ingestManual = onCall(
  {
    maxInstances: 1,
    memory: "512MiB",
  },
  async (request): Promise<{ status: string }> => {
    const data = request.data as { url?: string };
    if (!data.url) {
      throw new HttpsError("invalid-argument", "URL is required");
    }

    // Placeholder: real implementation requires legal review and vector DB setup.
    console.log("Ingestion requested for:", data.url);
    return { status: "not_implemented" };
  }
);
