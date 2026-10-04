
with open('frontend/src/app/app.module.ts', 'r', encoding='utf-8') as f:
    text = f.read()

if 'MobileScannerComponent' not in text:
    text = text.replace('import { NgModule } from \\'@angular/core\\';', 'import { NgModule } from \\'@angular/core\\';\\nimport { MobileScannerComponent } from \\'./components/mobile-scanner/mobile-scanner.component\\';')
    text = text.replace('declarations: [', 'declarations: [\\n    MobileScannerComponent,')
    with open('frontend/src/app/app.module.ts', 'w', encoding='utf-8') as f:
        f.write(text)

with open('frontend/src/app/app-routing.module.ts', 'r', encoding='utf-8') as f:
    text = f.read()

if 'MobileScannerComponent' not in text:
    text = text.replace('import { NgModule } from \\'@angular/core\\';', 'import { NgModule } from \\'@angular/core\\';\\nimport { MobileScannerComponent } from \\'./components/mobile-scanner/mobile-scanner.component\\';')
    text = text.replace('const routes: Routes = [', 'const routes: Routes = [\\n  { path: \\'mobile-scanner/:sessionId\\', component: MobileScannerComponent },')
    with open('frontend/src/app/app-routing.module.ts', 'w', encoding='utf-8') as f:
        f.write(text)

