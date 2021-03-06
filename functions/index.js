const admin = require("firebase-admin");
admin.initializeApp();
const functions = require("firebase-functions");
const db = admin.firestore();
const rtdb = admin.database();

exports.https_function = functions.https.onCall((data, context) => {
  // gettingData();
  // response.status(200).send(data);

  const text = data.text;
  if (!(typeof text === "string") || text.length === 0) {
    throw new functions.https.HttpsError("invalid-argument",
        "The function must be called with " +
        "one arguments text containing the message text to add.");
  }
  // Checking that the user is authenticated.
  if (!context.auth) {
    // Throwing an HttpsError so that the client gets the error details.
    throw new functions.https.HttpsError("failed-precondition",
        "The function must be called " +
        "while authenticated.");
  }
  // [START authIntegration]
  // Authentication / user information is automatically added to the request.
  const uid = context.auth.uid;
  const email = context.auth.token.email || null;
  const name = context.auth.token.name || null;
  const picture = context.auth.token.picture || null;
  // gettingData(uid, email);
  trying(uid, email);
  // [END authIntegration]
  return admin.database().ref("/messages").push({
    text: text,
    user: {uid, name, picture, email},
  }).then(() => {
    console.log("New Message written");
    // Returning the sanitized message to the client.
    return {text: text};
  })
  // [END returnMessageAsync]
      .catch((error) => {
        throw new functions.https.HttpsError("unknown", error.message, error);
      });
  // [END_EXCLUDE]
});


exports.https_function_dummy = functions.https.onCall((data, context) => {
  gettingData("uid", "email");
});


async function trying(uid, email) {
  console.log(uid);
  console.log(email);
  const citiesRef = db.collection("maquinasRegistradas");
  const snapshot = await citiesRef.orderBy("fecha", "desc").limit(100).get();
  if (snapshot.empty) {
    console.log("No matching documents.");
    return;
  }
  const valores = [];
  snapshot.forEach((doc) => {
    // console.log(doc.data());
    if (doc.data()["usuario"]==uid) {
      valores.push(doc.data());
    }
  });

  const sucs = await getSucursalesUsuario(email);
  const tXsucs = await realizarCalculos(valores, sucs);
  console.log(tXsucs);
  admin.database().ref("/messages").push({
    text: "text",
    user: {uid, email},
    tXsucs: tXsucs,
  }).then(() => {
    console.log("New Message written");
    // Returning the sanitized message to the client.
  })
  // [END returnMessageAsync]
      .catch((error) => {
        throw new functions.https.HttpsError("unknown", error.message, error);
      });
}


async function gettingData(uid, email) {
  const citiesRef = db.collection("maquinasRegistradas");
  const snapshot = await citiesRef.orderBy("fecha", "desc").limit(100).get();
  if (snapshot.empty) {
    console.log("No matching documents.");
    return;
  }
  const valores = [];
  snapshot.forEach((doc) => {
    // console.log(doc.data());
    if (doc.data()["usuario"]==uid) {
      valores.push(doc.data());
    }
  });

  console.log(valores);
  const sucs = await getSucursalesUsuario(email);

  const tXsucs = await realizarCalculos(valores, sucs);

  const totales = await calculoTotal(tXsucs);
  // console.log(totales);
  const tots = new Map();
  tots.set("sucursales", tXsucs);
  tots.set("totales", totales);
  const reporte = await generarReporte(tXsucs, totales);
  console.log(reporte);
  await updateDB(tXsucs, totales);
}

async function updateDB(tXsucs, totalFinal) {
  const dt = new Date(Date.now());
  const y = dt.getFullYear();
  const mt = dt.getMonth()+1;
  const d = dt.getDate();
  const fecha = y+"_"+mt+"_"+d;
  const totales = rtdb.ref("calculosSemanales/"+fecha);
  const totalessdb = db.collection("calculosSemanales");
  let calcs = "{";
  let aux = "";
  for (const [key, value] of tXsucs) {
    calcs += "\"" + key+"\": ";
    aux = "{";
    for (const [k, v] of value) {
      aux += "\"" + k + "\": " + v + ",";
    }
    aux = aux.substring(0, aux.length - 1);
    aux += "},";
    calcs += aux;
  }
  aux = "\"TOTAL\": {";
  for (const [key, value] of totalFinal) {
    aux += "\"" + key +"\": "+ value +", ";
  }
  aux = aux.substring(0, aux.length-2) + "}";
  calcs += aux + "}";
  console.log(calcs);
  const calculos = JSON.parse(calcs);
  totales.set({
    calculos,
  });
  totalessdb.doc(fecha).set(calculos);
}

async function generarReporte(tXsuc, totales) {
  let reporte = "\nTotales por sucursal:";
  // Aqui crear un for para ir formando el reporte por cada miembro del hashmap
  for (const [key, value] of tXsuc) {
    // console.log("\n" + key);
    reporte += "\n" + key;
    for (const [k, v] of value) {
      reporte += "\n   " + k + ": " + v;
    }
  }

  const totalFinal = await sumaDineroPrices(totales);
  reporte += "\n\nTOTAL A DEPOSITAR: *" + totalFinal.get("dinero") + "*" +
              "\nPremios ganados: "+totalFinal.get("prices");
  return reporte;
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
    const sucValor = valor["alias"].charAt(0)+valor["alias"].charAt(1);
    for (const [key, value] of Object.entries(valor)) {
      if (key.startsWith("*")) {
        temp.set(key, value);
      }
    }
    if (sucursales.includes(sucValor)) {
      if (!mapSem1.has(valor["alias"])) {
        mapSem1.set(valor["alias"], temp);
      } else if (!mapSem2.has(valor["alias"])) {
        mapSem2.set(valor["alias"], temp);
      }
    }
  });
  // RESTAS
  const restas = realizarRestas(mapSem1, mapSem2);
  console.log(restas);
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
    console.log(valor);
    if (valor.size > 0) {
      valor.forEach(function(val, cve) {
        if (c.has(cve)) {
          valContsFinal.set(cve, valor.get(cve) * c.get(cve).get("m"));
        } else {
          valContsFinal.set(cve, valor.get(cve));
        }
      });
      calcFinal.set(clave, valContsFinal);
    }
  });

  const mapAsc = new Map([...calcFinal.entries()].sort());
  console.log(mapAsc);
  return mapAsc;
}

async function dineroPorDepositar(resultados) {
  // const res = await db.collection("totales").doc("hoy").set(data);
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
  console.log(tXsuc);
  admin.database().ref("/depositar").push({
    text: "text",
    user: "{uid, email}",
    tXsuc: tXsuc,
  }).then(() => {
    console.log("New Message written");
    // Returning the sanitized message to the client.
  });
  return tXsuc;
}

async function sumaDineroPrices(totales) {
  let dinero = 0;
  let prices = 0;
  for (const [key, value] of totales) {
    // console.log(key, value)
    console.log(value);
    if (key=="*prizes") {
      prices += totales.get(key);
    } else {
      dinero += totales.get(key);
    }
  }
  const totalFinal = new Map();
  totalFinal.set("dinero", dinero);
  totalFinal.set("prices", prices);

  return totalFinal;
}

async function calculoTotal(tXsucs) {
  const totales = new Map();
  for (const [key] of tXsucs) {
    for (const [k, v] of tXsucs.get(key)) {
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
