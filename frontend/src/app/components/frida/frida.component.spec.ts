import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FridaComponent } from './frida.component';

describe('FridaComponent', () => {
  let component: FridaComponent;
  let fixture: ComponentFixture<FridaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FridaComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FridaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
