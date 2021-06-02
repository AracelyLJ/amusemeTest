const admin = require("firebase-admin");
admin.initializeApp();
const functions = require("firebase-functions");
const db = admin.firestore();

exports.https_function = functions.https.onRequest((request, response) => {
  gettingData();
  //whatsapp()
  response.status(200).send({data: "hey"});
});
async function gettingData() {
  const citiesRef = db.collection("maquinasRegistradas");
  const snapshot = await citiesRef.orderBy("fecha", "desc").limit(100).get();
  if (snapshot.empty) {
    console.log("No matching documents.");
    return;
  }
  const valores = [];
  snapshot.forEach((doc) => {
    valores.push(doc.data());
  });

  const sucs = await getSucursalesUsuario("gencovending1@gmail.com");

  const tXsucs = await realizarCalculos(valores, sucs);
  console.log(tXsucs);

  const totales = await calculoTotal(tXsucs);
  console.log(totales);
  const tots = new Map();
  tots.set("sucursales", tXsucs);
  tots.set("totales", totales);
  return tots;
}

// PARA LOS CÁLCULOS
async function getSucursalesUsuario(user) {
  // "gencovending1@gmail.com"
  const usuario = db.collection("usuarios");
  const snapshot = await usuario.where("correo", "==", user).get();
  if (snapshot.empty) {
    console.log("No matching documents.");
    return;
  }
  let sucursales = [];
  snapshot.forEach((doc) => {
    sucursales = doc.data()["sucursales"].split(",");
  });
  return sucursales;
}

async function realizarCalculos(valores, sucursales) {
  const mapSem1 = new Map();
  const mapSem2 = new Map();
  valores.forEach(function(valor) {
    const temp = new Map();
    for (const [key, value] of Object.entries(valor)) {
      if (key.startsWith("*")) {
        temp.set(key, value);
      }
    }
    // TO DO: Revisar que sean las sucursales correctas
    if (!mapSem1.has(valor["alias"])) {
      mapSem1.set(valor["alias"], temp);
    } else if (!mapSem2.has(valor["alias"])) {
      mapSem2.set(valor["alias"], temp);
    }
  });

  // RESTAS
  const restas = realizarRestas(mapSem1, mapSem2);
  // MULTIPLICADORES Y DIVISORES
  const resultados = await aplicarMultiplicadores(restas);
  // DINERO POR DEPOSITAR
  return await dineroPorDepositar(resultados);
}

async function realizarRestas(mapSem1, mapSem2) {
  const restas = new Map();
  for (const [clave, valor] of mapSem2) {
    const temp = new Map();
    for (const [c, v] of valor) {
      const r = Number(mapSem1.get(clave).get(c)) - Number(v);
      temp.set(c, r);
    }
    restas.set(clave, temp);
  }
  return restas;
}

async function aplicarMultiplicadores(restas) {
  const tipoMaquina = db.collection("tipoMaquina");
  const snapshot = await tipoMaquina.get();
  const tipoMaq = [];
  snapshot.forEach((doc) => {
    tipoMaq.push(doc.data());
  });

  const multis = new Map();
  tipoMaq.forEach(function(tm) {
    multis.set(tm["clave"], tm["contadores"]);
  });

  const conts = new Map();
  for (const [key, value] of multis) {
    const x = value.split(",");
    const c = new Map();
    for (let i = 0; i < x.length; i+=3) {
      const temp = new Map();
      temp.set("m", x[i+1]);
      temp.set("d", x[i+2]);
      c.set(x[0], temp);
    }
    conts.set(key, c);
  }

  const calcFinal = new Map();
  (await restas).forEach(function(valor, clave) {
    const key = clave.charAt(2) + clave.charAt(3);
    const c = conts.get(key);
    const valContsFinal = new Map();
    valor.forEach(function(val, cve) {
      if (c.has(cve)) {
        valContsFinal.set(cve, valor.get(cve) * c.get(cve).get("m"));
      } else {
        valContsFinal.set(cve, valor.get(cve));
      }
    });
    calcFinal.set(clave, valContsFinal);
  });

  const mapAsc = new Map([...calcFinal.entries()].sort());
  return mapAsc;
}

async function dineroPorDepositar(resultados) {
  const tXsuc = new Map();
  for (const [key, value] of resultados) {
    const cve = key.charAt(0) + key.charAt(1);
    const dicAux = new Map();
    if (tXsuc.has(cve)) {
      for (const [k, v] of value) {
        const valAux = tXsuc.get(cve).get(k);
        dicAux.set(k, valAux+v);
      }
      tXsuc.set(cve, dicAux);
    } else {
      for (const [k, v] of value) {
        dicAux.set(k, v);
      }
      tXsuc.set(cve, dicAux);
    }
  }
  return tXsuc;
}

async function calculoTotal(tXsuc) {
  const totales = new Map();
  for (const [key] of tXsuc) {
    for (const [k, v] of tXsuc.get(key)) {
      if (totales.has(k)) {
        const aux = v;
        totales.set(k, aux + totales.get(k));
      } else {
        totales.set(k, v);
      }
    }
  }
  return totales;
}

async function whatsapp(){
  var client = require("twilio")("ACb540a5ac308a9e20f7d5734ca0b82fea", "ed0469a9e5f65b9e2efc36f57b054e78");
  console.log("sending msg");

  client.messages.create({
    from: "whatsapp:+14155238886",
    body: "hola",
    to: "whatsapp:+5214751073063"
  }).then(message => console.log(message.sid));
}