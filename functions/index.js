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
  const reporte = await generarReporte(tXsucs, totales);
  console.log(reporte);
  await updateDB(tXsucs, totales);
}

async function updateDB(tXsucs, totalFinal) {
  const dt = new Date(Date.now());
  const y = dt.getFullYear();
  const mt = dt.getMonth()+1;
  const d = dt.getDate();
  const x = new Date();
  const h = x.getHours();
  const m = x.getMinutes();
  const s = x.getSeconds();
  const fecha = y+"_"+mt+"_"+d+"_"+h+":"+m+":"+s;
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
  for (const [key, value] of totalFinal){
    aux += "\"" + key +"\": "+ value +", "
  }
  aux = aux.substring(0, aux.length-2) + "}";
  calcs += aux + "}"
  console.log(calcs);
  const calculos = JSON.parse(calcs);
  totales.set({
    calculos
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
  console.log(tXsuc);
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
