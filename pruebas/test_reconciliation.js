const fs = require('fs');

// Load API
const apiSrc = fs.readFileSync('js/api.js', 'utf8');
const fnApi = new Function(apiSrc + '; return { DEMO_DATA, FinanceAPI };');
const { DEMO_DATA, FinanceAPI } = fnApi();

// Load NLP
const nlpSrc = fs.readFileSync('js/nlp_parser.js', 'utf8');
const fnNlp = new Function(nlpSrc + '; return FinancialNLP;');
const FinancialNLP = fnNlp();

// Test 1: NLP
console.log('--- TEST 1: NLP ---');
const nlpEpm = FinancialNLP.parseIntent('pague 197.000 epm');
console.log('NLP EPM isFixed:', nlpEpm.isFixed, 'idFijo:', nlpEpm.idFijo, 'amount:', nlpEpm.amount);
if (nlpEpm.isFixed && nlpEpm.idFijo === 'F-0107' && nlpEpm.amount === 197000) {
  console.log('✅ NLP EPM PASSED');
} else {
  console.error('❌ NLP EPM FAILED');
}

// Test 2: DEMO_DATA initial state
console.log('\n--- TEST 2: DEMO_DATA State ---');
const epmFijo = DEMO_DATA.fijos.find(f => f.id === 'F-0107');
console.log('F-0107 in DEMO_DATA:', epmFijo);
console.log('DEMO_DATA fijos_pendientes:', DEMO_DATA.resumen.fijos_pendientes);
console.log('DEMO_DATA disponible:', DEMO_DATA.resumen.disponible);
console.log('DEMO_DATA gastos:', DEMO_DATA.resumen.gastos);

// Test 3: Reconciliation simulation
console.log('\n--- TEST 3: Reconciliation Logic ---');
const dummyState = {
  activeMonth: 'octubre',
  data: JSON.parse(JSON.stringify(DEMO_DATA))
};

// Simulate reconcileFixedObligationsWithMovements
const movs = dummyState.data.movimientos || [];
const activeMonth = dummyState.activeMonth;
const targetMonth = activeMonth === 'all' ? 'octubre' : activeMonth;
const monthExpenses = movs.filter(m => !String(m.tipo || '').toLowerCase().includes('ingreso') && String(m.mes || '').toLowerCase() === targetMonth);

dummyState.data.fijos.forEach(f => {
  const cLower = String(f.concepto || '').toLowerCase();
  const idUpper = String(f.id || '').toUpperCase();

  const matchingMov = monthExpenses.find(m => {
    const d = String(m.descripcion || '').toLowerCase();
    const s = String(m.subcategoria || '').toLowerCase();
    const ref = String(m.notas || '').toUpperCase();

    if (ref.includes(idUpper)) return true;
    if (m.idFijo && String(m.idFijo).toUpperCase() === idUpper) return true;

    if (idUpper === 'F-0107' && (d.includes('epm') || s.includes('epm') || d.includes('energia') || d.includes('luz'))) return true;
    if (idUpper === 'F-0109' && (d.includes('tigo') || s.includes('internet') || d.includes('wifi'))) return true;
    if (idUpper === 'F-0114' && (d.includes('spotify'))) return true;
    if (idUpper === 'F-0115' && (d.includes('datos') || d.includes('celular') || s.includes('telefonia'))) return true;
    if (idUpper === 'F-0116' && (d.includes('arriendo'))) return true;
    if (idUpper === 'F-0117' && (d.includes('google'))) return true;
    return false;
  });

  if (matchingMov) {
    const val = Number(matchingMov.valor || 0);
    if (val > 0) {
      const esVariable = /\b(epm|energia|luz|agua|gas|tigo|internet|wifi|gasolina)\b/i.test(cLower);
      if (esVariable || val > Number(f.cuota || 0)) {
        f.cuota = val;
      }
      f.abonado = Math.max(Number(f.abonado || 0), val);
      if (f.abonado >= Number(f.cuota || 0) || esVariable) {
        f.abonado = f.cuota;
        f.estado = '🟢 Pagado';
      } else {
        f.estado = '🟡 Abono Parcial';
      }
      f.fechaPago = matchingMov.fecha || '17/10/2026';
    }
  }
});

const pendFijos = dummyState.data.fijos.filter(f => !String(f.estado).includes('Pagado') && Number(f.cuota || 0) > Number(f.abonado || 0));
const totalPend = pendFijos.reduce((acc, f) => acc + Math.max(0, Number(f.cuota || 0) - Number(f.abonado || 0)), 0);

console.log('Reconciled pending obligations count:', pendFijos.length, '(expected 12)');
console.log('Reconciled total pending COP:', totalPend, '(expected 2380000)');
const epmAfter = dummyState.data.fijos.find(f => f.id === 'F-0107');
console.log('EPM status:', epmAfter.estado, 'cuota:', epmAfter.cuota, 'abonado:', epmAfter.abonado);

if (pendFijos.length === 12 && totalPend === 2380000 && epmAfter.estado === '🟢 Pagado') {
  console.log('✅ RECONCILIATION FULLY VERIFIED AND PASSING!');
} else {
  console.error('❌ RECONCILIATION FAILED');
}
