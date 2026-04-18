import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmpleadoTratadorComponent } from './empleado-tratador.component';

describe('EmpleadoTratadorComponent', () => {
  let component: EmpleadoTratadorComponent;
  let fixture: ComponentFixture<EmpleadoTratadorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmpleadoTratadorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EmpleadoTratadorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
