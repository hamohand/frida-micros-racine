import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FridaEditComponent } from './frida-edit.component';

describe('FridaEditComponent', () => {
  let component: FridaEditComponent;
  let fixture: ComponentFixture<FridaEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FridaEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FridaEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
