const fs = require('fs');
const src = fs.readFileSync('js/nlp_parser.js', 'utf8');
const fn = new Function(src + '; return FinancialNLP;');
const FinancialNLP = fn();

const result = FinancialNLP.parseIntent('pague 197.000 epm');
console.log('Result:', JSON.stringify(result, null, 2));

const result2 = FinancialNLP.parseIntent('pague 120 mil tigo');
console.log('Result 2:', JSON.stringify(result2, null, 2));

const result3 = FinancialNLP.parseIntent('arriendo 900 mil');
console.log('Result 3:', JSON.stringify(result3, null, 2));
