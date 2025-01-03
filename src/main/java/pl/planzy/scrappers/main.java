package pl.planzy.scrappers;

import pl.planzy.scrappers.impl.ScrapperEbilet;
import pl.planzy.scrappers.impl.ScrapperGoingApp;

public class main {

    public static void main(String[] args) {

        Scrapper scrapper = new ScrapperGoingApp();

        scrapper.scrapeData();
    }
}
