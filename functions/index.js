const admin = require("firebase-admin");
admin.initializeApp();
const functions = require("firebase-functions");
const db = admin.firestore();
const rtdb = admin.database();

exports.https_function = functions.https.onRequest((request, response) => {
  gettingData();
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
  
  const totales = await calculoTotal(tXsucs);
  // console.log(totales);
  const tots = new Map();
  tots.set("sucursales", tXsucs);
  tots.set("totales", totales);
  reporte = await generarReporte(tXsucs, totales);
  await updateDB(tXsucs, totales);

}

async function updateDB(tXsucs, total_final) {
  var d = new Date(Date.now());
  y = d.getFullYear();
  m = d.getMonth()+1;
  d = d.getDate();
  var x = new Date();
  h = x.getHours();
  m = x.getMinutes();
  s = x.getSeconds();
  const fecha = y+"_"+m+"_"+d+"_"+h+":"+m+":"+s;
  const totales = rtdb.ref('sucursalesRegistradas/'+fecha);

  totales.set({
    fecha: {
      date_of_birth: 'June 23, 1912',
      full_name: 'Alan Turing'
    }
  });

}

async function generarReporte(tXsuc, totales) {
  var reporte = "Totales por sucursal:\n";
  // Aqui crear un for para ir formando el reporte por cada miembro del hashmap
  for (const [key, value] of tXsuc) {
    // console.log("\n" + key);
    reporte += "\n" + key;
    for (const [k, v] of value){
        // console.log("\n" + k, v);
        reporte += "\n   " + k + ": " + v;
    }

  }

  total_final = await sumaDineroPrices(totales);
  
  
  reporte += "\n\nTOTAL A DEPOSITAR: *" + total_final.get("dinero") + "*"
              + "\nPremios ganados: "+total_final.get("prices");
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
  console.log(tXsuc)
  return tXsuc;
}

async function sumaDineroPrices(totales){
  var dinero = 0;
  var prices = 0;
  for (const [key, value] of totales){
    // console.log(key, value)
    if (key=="*prices"){
      prices += totales.get(key);
    }else{
      dinero += totales.get(key);
    }
  }
  total_final = new Map();
  total_final.set("dinero", dinero);
  total_final.set("prices", prices);

  return total_final;
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

async function whatsapp(msj) {
  const sid = "ACb540a5ac308a9e20f7d5734ca0b82fea";
  const token = "b1e48d39ace18f6b6a89499750e368b2";
  const client = require("twilio")(sid, token);
  console.log("sending msg");

  client.messages.create({
    from: "whatsapp:+14155238886",
    body: msj,
    to: "whatsapp:+5214751073063",
  }).then((message) => console.log(message.sid));
}
