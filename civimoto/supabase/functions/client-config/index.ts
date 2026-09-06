import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") || "";
  if (!supabaseUrl || !anonKey) {
    return new Response(JSON.stringify({ message: "Configuración CiviMoto incompleta" }), { status: 500, headers: cors });
  }
  return new Response(JSON.stringify({
    app: "CiviMoto",
    environment: "production",
    supabase_url: supabaseUrl,
    publishable_key: anonKey,
  }), { status: 200, headers: cors });
});
