package ru.work.service.dto.medical;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import ru.work.service.anotations.SheetColumn;
import ru.work.service.dto.FileDto;
import ru.work.service.dto.enums.ProcessedStatus;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class MedicalDocFile extends FileDto {

    private String header;
    private String subHeader;

    @SheetColumn(name = "Дата поступления материала", parseFromFormat = "dd.MM.yyyy")
    private LocalDate receiveMaterialDate;
    @SheetColumn(name = "Пациент")
    private String patient;
    @SheetColumn(name = "Биоматериал")
    private String bioMaterial;
    @SheetColumn(name = "Диагноз")
    private String diagnose;
    @SheetColumn(name = "ИБ")
    private String ib;
    @SheetColumn(name = "№ анализа")
    private String numberAnalyze;
    @SheetColumn(name = "Отделение")
    private String division;

    @Override
    public String toString() {
        return super.getSizeKb() + " КБ - " + super.getFilename();
    }

    @Override
    public Integer getCount() {
        if (this.microorganisms == null || this.getStatus() != ProcessedStatus.SUCCESS) return 0;
        return this.microorganisms.size();
    }

    private LinkedList<Microorganism> microorganisms;    //№	Выделенные микроорганизмы	КОЕ/мл
    private List<AntibioticGram> antibioticGrams;       //Антибиотикограмма

    private String outMaterialDate;                      //Дата выдачи

    public MedicalDocFile(FileDto file) {
        super(file);
        this.microorganisms = new LinkedList<>();
        this.antibioticGrams = new LinkedList<>();
    }

    public void addAntibioticGram(String header) {
        this.antibioticGrams.add(new AntibioticGram(header, new LinkedList<>()));
    }

    public void addAntibioticGramItem(String header, String name, String[] result) {
        AntibioticGram antibioticGram = this.antibioticGrams.stream()
                .filter(a -> a.header.equalsIgnoreCase(header)).findFirst()
                .orElse(new AntibioticGram(header, new LinkedList<>()));
        antibioticGram.items.add(new AntibioticGram.AntibioticoGramItem(StringUtils.trim(name), result));
    }

    public void addMicroorganism(String name, String count) {
        String q = count.substring(count.length() - 1);
        this.microorganisms.add(new Microorganism(name, "10^" + q));
    }

}
